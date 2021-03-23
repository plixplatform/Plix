package com.plixplatform.state.appender

import cats.data.EitherT
import com.plixplatform.block.MicroBlock
import com.plixplatform.lang.ValidationError
import com.plixplatform.metrics.{BlockStats, _}
import com.plixplatform.network.MicroBlockSynchronizer.MicroblockData
import com.plixplatform.network._
import com.plixplatform.state.Blockchain
import com.plixplatform.transaction.BlockchainUpdater
import com.plixplatform.transaction.TxValidationError.InvalidSignature
import com.plixplatform.utils.ScorexLogging
import com.plixplatform.utx.UtxPool
import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import kamon.Kamon
import monix.eval.Task
import monix.execution.Scheduler

import scala.util.{Left, Right}

object MicroblockAppender extends ScorexLogging {
  def apply(blockchainUpdater: BlockchainUpdater with Blockchain, utxStorage: UtxPool, scheduler: Scheduler, verify: Boolean = true)(
      microBlock: MicroBlock): Task[Either[ValidationError, Unit]] = {

    Task(metrics.microblockProcessingTimeStats.measureSuccessful {
      blockchainUpdater
        .processMicroBlock(microBlock, verify)
        .map(_ => utxStorage.removeAll(microBlock.transactionData))
    }).executeOn(scheduler)
  }

  def apply(blockchainUpdater: BlockchainUpdater with Blockchain,
            utxStorage: UtxPool,
            allChannels: ChannelGroup,
            peerDatabase: PeerDatabase,
            scheduler: Scheduler)(ch: Channel, md: MicroblockData): Task[Unit] = {
    import md.microBlock
    val microblockTotalResBlockSig = microBlock.totalResBlockSig
    (for {
      _                <- EitherT(Task.now(microBlock.signaturesValid()))
      validApplication <- EitherT(apply(blockchainUpdater, utxStorage, scheduler)(microBlock))
    } yield validApplication).value.map {
      case Right(()) =>
        md.invOpt match {
          case Some(mi) => allChannels.broadcast(mi, except = md.microblockOwners())
          case None     => log.warn(s"${id(ch)} Not broadcasting MicroBlockInv")
        }
        BlockStats.applied(microBlock)
      case Left(is: InvalidSignature) =>
        peerDatabase.blacklistAndClose(ch, s"Could not append microblock $microblockTotalResBlockSig: $is")
      case Left(ve) =>
        BlockStats.declined(microBlock)
        log.debug(s"${id(ch)} Could not append microblock $microblockTotalResBlockSig: $ve")
    }
  }

  private[this] object metrics {
    val microblockProcessingTimeStats = Kamon.timer("microblock-appender.processing-time")
  }
}
