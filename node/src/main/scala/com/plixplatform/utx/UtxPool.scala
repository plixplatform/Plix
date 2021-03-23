package com.plixlatform.utx

import com.plixlatform.account.Address
import com.plixlatform.common.state.ByteStr
import com.plixlatform.lang.ValidationError
import com.plixlatform.mining.MultiDimensionalMiningConstraint
import com.plixlatform.state.Portfolio
import com.plixlatform.transaction._
import com.plixlatform.transaction.smart.script.trace.TracedResult

import scala.concurrent.duration.Duration

trait UtxPool extends AutoCloseable {
  def putIfNew(tx: Transaction, verify: Boolean = true): TracedResult[ValidationError, Boolean]

  def removeAll(txs: Traversable[Transaction]): Unit

  def spendableBalance(addr: Address, assetId: Asset): Long

  def pessimisticPortfolio(addr: Address): Portfolio

  def all: Seq[Transaction]

  def size: Int

  def transactionById(transactionId: ByteStr): Option[Transaction]

  def packUnconfirmed(rest: MultiDimensionalMiningConstraint, maxPackTime: Duration): (Seq[Transaction], MultiDimensionalMiningConstraint)
}
