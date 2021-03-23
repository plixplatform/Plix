package com.plixplatform.history

import com.plixplatform.account.Address
import com.plixplatform.block.Block
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.database.LevelDBWriter
import com.plixplatform.state._
import com.plixplatform.state.extensions.Distributions
import com.plixplatform.transaction.{BlockchainUpdater, DiscardedBlocks, DiscardedTransactions, Transaction}
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.Duration

//noinspection ScalaStyle
case class Domain(blockchainUpdater: BlockchainUpdater with NG, levelDBWriter: LevelDBWriter) {
  def effBalance(a: Address): Long = blockchainUpdater.effectiveBalance(a, 1000)

  def appendBlock(b: Block): Option[DiscardedTransactions] = blockchainUpdater.processBlock(b).explicitGet()

  def removeAfter(blockId: ByteStr): DiscardedBlocks = blockchainUpdater.removeAfter(blockId).explicitGet()

  def lastBlockId: ByteStr = blockchainUpdater.lastBlockId.get

  def portfolio(address: Address): Portfolio = Distributions(blockchainUpdater).portfolio(address)

  def addressTransactions(address: Address): Seq[(Height, Transaction)] =
    blockchainUpdater.addressTransactionsObservable(address, Set.empty).take(128).toListL.runSyncUnsafe(Duration.Inf)

  def carryFee: Long = blockchainUpdater.carryFee
}
