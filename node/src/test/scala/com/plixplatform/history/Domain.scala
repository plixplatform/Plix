package com.plixlatform.history

import com.plixlatform.account.Address
import com.plixlatform.block.Block
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.database.LevelDBWriter
import com.plixlatform.state._
import com.plixlatform.state.extensions.Distributions
import com.plixlatform.transaction.{BlockchainUpdater, DiscardedBlocks, DiscardedTransactions, Transaction}
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
