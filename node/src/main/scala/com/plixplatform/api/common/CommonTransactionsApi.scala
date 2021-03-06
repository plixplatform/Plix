package com.plixplatform.api.common

import com.plixplatform.account.Address
import com.plixplatform.common.state.ByteStr
import com.plixplatform.lang.ValidationError
import com.plixplatform.protobuf.transaction.VanillaTransaction
import com.plixplatform.state.diffs.FeeValidation
import com.plixplatform.state.diffs.FeeValidation.FeeDetails
import com.plixplatform.state.{Blockchain, Height}
import com.plixplatform.transaction.Asset
import com.plixplatform.transaction.smart.script.trace.TracedResult
import com.plixplatform.utx.UtxPool
import com.plixplatform.wallet.Wallet
import monix.reactive.Observable

private[api] class CommonTransactionsApi(blockchain: Blockchain, utx: UtxPool, wallet: Wallet, broadcast: (VanillaTransaction, Boolean) => Unit) {

  def transactionsByAddress(address: Address, fromId: Option[ByteStr] = None): Observable[(Height, VanillaTransaction)] =
    blockchain.addressTransactionsObservable(address, Set.empty, fromId)

  def transactionById(transactionId: ByteStr): Option[(Int, VanillaTransaction)] =
    blockchain.transactionInfo(transactionId)

  def unconfirmedTransactions(): Seq[VanillaTransaction] =
    utx.all

  def unconfirmedTransactionById(transactionId: ByteStr): Option[VanillaTransaction] =
    utx.transactionById(transactionId)

  def calculateFee(tx: VanillaTransaction): Either[ValidationError, (Asset, Long, Long)] = {
    FeeValidation
      .getMinFee(blockchain, blockchain.height, tx)
      .map {
        case FeeDetails(asset, _, feeInAsset, feeInPlix) =>
          (asset, feeInAsset, feeInPlix)
      }

  }

  def broadcastTransaction(tx: VanillaTransaction): TracedResult[ValidationError, VanillaTransaction] = {
    val result = for {
      isNew <- utx.putIfNew(tx)
      _ = broadcast(tx, isNew)
    } yield tx

    result
  }

  def allTransactionById(transactionId: ByteStr): Option[(Int, VanillaTransaction)] =
    utx.transactionById(transactionId) match {
      case Some(value) => Some((0, value))
      case None =>
        blockchain.transactionInfo(transactionId) match {
          case Some(value) => Some(value)
          case None        => None
        }
    }
}
