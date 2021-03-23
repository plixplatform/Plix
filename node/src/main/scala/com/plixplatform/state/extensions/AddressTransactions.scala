package com.plixplatform.state.extensions

import com.plixplatform.account.Address
import com.plixplatform.block.Block.BlockId
import com.plixplatform.common.state.ByteStr
import com.plixplatform.state.Height
import com.plixplatform.transaction.{Transaction, TransactionParser}
import monix.reactive.Observable

trait AddressTransactions {
  def addressTransactionsObservable(address: Address,
                                    types: Set[TransactionParser],
                                    fromId: Option[ByteStr] = None): Observable[(Height, Transaction)]
}

object AddressTransactions {
  def apply[T](value: T)(implicit ev: T => AddressTransactions): AddressTransactions = value

  trait Prov[T] {
    def addressTransactions(value: T): AddressTransactions
  }

  case object Empty extends AddressTransactions {
    override def addressTransactionsObservable(address: Address,
                                               types: Set[TransactionParser],
                                               fromId: Option[BlockId]): Observable[(Height, Transaction)] =
      Observable.empty
  }
}
