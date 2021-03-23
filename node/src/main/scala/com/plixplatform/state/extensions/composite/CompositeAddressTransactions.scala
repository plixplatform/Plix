package com.plixplatform.state.extensions.composite

import com.plixplatform.account.Address
import com.plixplatform.common.state.ByteStr
import com.plixplatform.state.extensions.AddressTransactions
import com.plixplatform.state.{Diff, Height}
import com.plixplatform.transaction.{Transaction, TransactionParser}
import monix.reactive.Observable

private[state] final class CompositeAddressTransactions(baseProvider: AddressTransactions, height: Height, getDiff: () => Option[Diff])
    extends AddressTransactions {
  override def addressTransactionsObservable(address: Address,
                                             types: Set[TransactionParser],
                                             fromId: Option[ByteStr]): Observable[(Height, Transaction)] = {
    val fromDiff = for {
      diff                    <- getDiff().toIterable
      (height, tx, addresses) <- diff.transactions.values.toVector.reverse
    } yield (Height(height), tx, addresses)

    com.plixplatform.state.addressTransactionsCompose(baseProvider, Observable.fromIterable(fromDiff))(address, types, fromId)
  }
}
