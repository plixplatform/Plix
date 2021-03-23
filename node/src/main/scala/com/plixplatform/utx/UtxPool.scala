package com.plixplatform.utx

import com.plixplatform.account.Address
import com.plixplatform.common.state.ByteStr
import com.plixplatform.lang.ValidationError
import com.plixplatform.mining.MultiDimensionalMiningConstraint
import com.plixplatform.state.Portfolio
import com.plixplatform.transaction._
import com.plixplatform.transaction.smart.script.trace.TracedResult

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
