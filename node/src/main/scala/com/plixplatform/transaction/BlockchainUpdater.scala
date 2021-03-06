package com.plixplatform.transaction
import com.plixplatform.block.Block.BlockId
import com.plixplatform.block.{Block, MicroBlock}
import com.plixplatform.common.state.ByteStr
import com.plixplatform.lang.ValidationError
import monix.reactive.Observable

trait BlockchainUpdater {
  def processBlock(block: Block, verify: Boolean = true): Either[ValidationError, Option[DiscardedTransactions]]
  def processMicroBlock(microBlock: MicroBlock, verify: Boolean = true): Either[ValidationError, Unit]
  def removeAfter(blockId: ByteStr): Either[ValidationError, DiscardedBlocks]
  def lastBlockInfo: Observable[LastBlockInfo]
  def isLastBlockId(id: ByteStr): Boolean
  def shutdown(): Unit
}

case class LastBlockInfo(id: BlockId, height: Int, score: BigInt, ready: Boolean)
