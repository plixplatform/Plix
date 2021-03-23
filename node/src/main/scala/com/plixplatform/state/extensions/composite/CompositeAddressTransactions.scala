package com.plixlatform.state.extensions.composite

import com.plixlatform.account.Address
import com.plixlatform.common.state.ByteStr
import com.plixlatform.state.extensions.AddressTransactions
import com.plixlatform.state.{Diff, Height}
import com.plixlatform.transaction.{Transaction, TransactionParser}
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

    com.plixlatform.state.addressTransactionsCompose(baseProvider, Observable.fromIterable(fromDiff))(address, types, fromId)
  }
}
