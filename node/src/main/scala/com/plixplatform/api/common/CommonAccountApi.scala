package com.plixlatform.api.common

import com.plixlatform.account.Address
import com.plixlatform.api.http.AddressApiRoute.DataAndID
import com.plixlatform.common.state.ByteStr
import com.plixlatform.lang.script.Script
import com.plixlatform.state.diffs.FeeValidation
import com.plixlatform.state.{Blockchain, BlockchainExt, DataEntry, Height}
import com.plixlatform.transaction.{Asset, DataTransaction}
import com.plixlatform.transaction.Asset.IssuedAsset
import com.plixlatform.transaction.assets.IssueTransaction
import com.plixlatform.transaction.lease.{LeaseTransaction, LeaseTransactionV1, LeaseTransactionV2}
import monix.reactive.Observable

class CommonAccountApi(blockchain: Blockchain) {
  import CommonAccountApi._

  def balance(address: Address, confirmations: Int = 0): Long =
    blockchain.balance(address, blockchain.height, confirmations)

  def effectiveBalance(address: Address, confirmations: Int = 0): Long =
    blockchain.effectiveBalance(address, confirmations)

  def balanceDetails(address: Address): BalanceDetails = {
    val portfolio = blockchain.plixPortfolio(address)
    BalanceDetails(
      portfolio.balance,
      blockchain.generatingBalance(address),
      portfolio.balance - portfolio.lease.out,
      portfolio.effectiveBalance,
      portfolio.lease.in,
      portfolio.lease.out
    )
  }

  def assetBalance(address: Address, asset: IssuedAsset): Long =
    blockchain.balance(address, asset)

  def portfolio(address: Address): Map[Asset, Long] = {
    val portfolio = blockchain.portfolio(address)
    portfolio.assets ++ Map(Asset.Plix -> portfolio.balance)
  }

  def portfolioNFT(address: Address, from: Option[IssuedAsset]): Observable[IssueTransaction] =
    blockchain.nftObservable(address, from)

  def script(address: Address): AddressScriptInfo = {
    val script: Option[Script] = blockchain.accountScript(address)

    AddressScriptInfo(
      script = script.map(_.bytes()),
      scriptText = script.map(_.expr.toString), // [WAIT] script.map(Script.decompile),
      complexity = script.map(_.complexity).getOrElse(0),
      extraFee = if (script.isEmpty) 0 else FeeValidation.ScriptExtraFee
    )
  }

  def data(address: Address, key: String): Option[DataEntry[_]] =
    blockchain.accountData(address, key)

  def dataStream(address: Address, keyFilter: String => Boolean = _ => true): Observable[DataEntry[_]] =
    Observable
      .defer(Observable.fromIterable(concurrent.blocking(blockchain.accountDataKeys(address))))
      .filter(keyFilter)
      .map(blockchain.accountData(address, _))
      .flatMap(Observable.fromIterable(_))

  def dataStreamAndId(address: Address, keyFilter: String => Boolean = _ => true): Observable[DataAndID] =
    for {
      data <- Observable
        .defer(Observable.fromIterable(concurrent.blocking(blockchain.accountDataKeys(address))))
        .filter(keyFilter)
        .map(blockchain.accountData(address, _))
        .flatMap(Observable.fromIterable(_))
      id <- getTxId(address, data.key)
    } yield {
      DataAndID(data, id)
    }

  def getTxId(address: Address, key: String): Observable[String] =
    blockchain
      .addressTransactionsObservable(address, Set.empty, None)
      .filter {
        case (_, dtx: DataTransaction) => dtx.data.exists(data => data.key == key)
        case (_, _)                    => ???
      }
      .map {
        case (_, dtx: DataTransaction) => dtx.id.value().base58
        case (_, _)                    => ???
      }
      .head

  def activeLeases(address: Address): Observable[(Height, LeaseTransaction)] =
    blockchain
      .addressTransactionsObservable(address, Set(LeaseTransactionV1, LeaseTransactionV2))
      .collect {
        case (height, leaseTransaction: LeaseTransaction) if blockchain.leaseDetails(leaseTransaction.id()).exists(_.isActive) =>
          (height, leaseTransaction)
      }
}

object CommonAccountApi {
  final case class BalanceDetails(regular: Long, generating: Long, available: Long, effective: Long, leaseIn: Long, leaseOut: Long)
  final case class AddressScriptInfo(script: Option[ByteStr], scriptText: Option[String], complexity: Long, extraFee: Long)
}
