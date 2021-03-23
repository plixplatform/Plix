package com.plixlatform.state.appender

import com.plixlatform.block.Block
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.consensus.PoSSelector
import com.plixlatform.lang.ValidationError
import com.plixlatform.metrics.{BlockStats, Metrics}
import com.plixlatform.mining.Miner
import com.plixlatform.network.{InvalidBlockStorage, PeerDatabase, formatBlocks, id}
import com.plixlatform.state._
import com.plixlatform.transaction.TxValidationError.GenericError
import com.plixlatform.transaction._
import com.plixlatform.utils.{ScorexLogging, Time}
import com.plixlatform.utx.UtxPoolImpl
import io.netty.channel.Channel
import monix.eval.{Coeval, Task}
import monix.execution.Scheduler
import org.influxdb.dto.Point

import scala.util.{Left, Right}

object ExtensionAppender extends ScorexLogging {

  def apply(blockchainUpdater: BlockchainUpdater with Blockchain,
            utxStorage: UtxPoolImpl,
            pos: PoSSelector,
            time: Time,
            invalidBlocks: InvalidBlockStorage,
            peerDatabase: PeerDatabase,
            miner: Miner,
            scheduler: Scheduler)(ch: Channel, extensionBlocks: Seq[Block]): Task[Either[ValidationError, Option[BigInt]]] = {
    def p(blocks: Seq[Block]): Task[Either[ValidationError, Option[BigInt]]] =
      Task(Signed.validateOrdered(blocks).flatMap { newBlocks =>
        {
          val extension = newBlocks.dropWhile(blockchainUpdater.contains)

          extension.headOption.map(_.reference) match {
            case Some(lastCommonBlockId) =>
              val forkApplicationResultEi = Coeval {
                extension.view
                  .map { b =>
                    b -> validateAndAppendBlock(blockchainUpdater, utxStorage, pos, time)(b).right
                      .map {
                        _.foreach(bh => BlockStats.applied(b, BlockStats.Source.Ext, bh))
                      }
                  }
                  .zipWithIndex
                  .collectFirst { case ((b, Left(e)), i) => (i, b, e) }
                  .fold[Either[ValidationError, Unit]](Right(())) {
                    case (i, declinedBlock, e) =>
                      e match {
                        case _: TxValidationError.BlockFromFuture =>
                        case _                                    => invalidBlocks.add(declinedBlock.uniqueId, e)
                      }

                      extension.view
                        .dropWhile(_ != declinedBlock)
                        .foreach(BlockStats.declined(_, BlockStats.Source.Ext))

                      if (i == 0) log.warn(s"Can't process fork starting with $lastCommonBlockId, error appending block $declinedBlock: $e")
                      else
                        log.warn(s"Processed only ${i + 1} of ${newBlocks.size} blocks from extension, error appending next block $declinedBlock: $e")

                      Left(e)
                  }
              }

              val initialHeight = blockchainUpdater.height

              val droppedBlocksEi = for {
                commonBlockHeight <- blockchainUpdater.heightOf(lastCommonBlockId).toRight(GenericError("Fork contains no common parent"))
                droppedBlocks <- {
                  if (commonBlockHeight < initialHeight)
                    blockchainUpdater.removeAfter(lastCommonBlockId)
                  else Right(Seq.empty)
                }
              } yield (commonBlockHeight, droppedBlocks)

              droppedBlocksEi.flatMap {
                case (commonBlockHeight, droppedBlocks) =>
                  forkApplicationResultEi() match {
                    case Left(e) =>
                      blockchainUpdater.removeAfter(lastCommonBlockId).explicitGet()
                      droppedBlocks.foreach(blockchainUpdater.processBlock(_).explicitGet())
                      Left(e)

                    case Right(_) =>
                      val depth = initialHeight - commonBlockHeight
                      if (depth > 0) {
                        Metrics.write(
                          Point
                            .measurement("rollback")
                            .addField("depth", initialHeight - commonBlockHeight)
                            .addField("txs", droppedBlocks.size)
                        )
                      }
                      droppedBlocks.flatMap(_.transactionData).foreach(utxStorage.putIfNew(_))
                      Right(Some(blockchainUpdater.score))
                  }
              }

            case None =>
              log.debug("No new blocks found in extension")
              Right(None)
          }
        }
      }).executeOn(scheduler)

    extensionBlocks.foreach(BlockStats.received(_, BlockStats.Source.Ext, ch))
    processAndBlacklistOnFailure(
      ch,
      peerDatabase,
      miner,
      s"${id(ch)} Attempting to append extension ${formatBlocks(extensionBlocks)}",
      s"${id(ch)} Successfully appended extension ${formatBlocks(extensionBlocks)}",
      s"${id(ch)} Error appending extension ${formatBlocks(extensionBlocks)}"
    )(p(extensionBlocks))
  }
}
