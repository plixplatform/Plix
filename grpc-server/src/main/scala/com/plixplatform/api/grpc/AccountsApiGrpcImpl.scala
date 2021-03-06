package com.plixplatform.api.grpc
import com.plixplatform.api.common.CommonAccountApi
import com.plixplatform.common.state.ByteStr
import com.plixplatform.protobuf.transaction.{AssetAmount, AssetId, PBTransactions}
import com.plixplatform.state.Blockchain
import com.plixplatform.transaction.Asset.IssuedAsset
import io.grpc.stub.StreamObserver
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.Future

class AccountsApiGrpcImpl(blockchain: Blockchain)(implicit sc: Scheduler) extends AccountsApiGrpc.AccountsApi {
  private[this] val commonApi = new CommonAccountApi(blockchain)

  override def getBalances(request: BalancesRequest, responseObserver: StreamObserver[BalanceResponse]): Unit = {
    val plixOption = if (request.assets.exists(_.asset.isPlix)) {
      val details = commonApi.balanceDetails(request.address.toAddress)
      Some(
        BalanceResponse.PlixBalances(details.regular, details.generating, details.available, details.effective, details.leaseIn, details.leaseOut))
    } else {
      None
    }

    val assetIdSet = request.assets.collect { case AssetId(AssetId.Asset.IssuedAsset(assetId)) => assetId }
    val assets =
      if (assetIdSet.isEmpty)
        Observable.empty
      else
        Observable
          .defer(Observable.fromIterable(commonApi.portfolio(request.address.toAddress)))
          .collect {
            case (IssuedAsset(assetId), balance) if request.assets.isEmpty || assetIdSet.contains(assetId.toPBByteString) =>
              AssetAmount(assetId, balance)
          }

    val resultStream = Observable
      .fromIterable(plixOption)
      .map(wb => BalanceResponse(request.address, BalanceResponse.Balance.Plix(wb)))
      .++(assets.map(am => BalanceResponse(request.address, BalanceResponse.Balance.Asset(am))))

    responseObserver.completeWith(resultStream)
  }

  override def getScript(request: AccountRequest): Future[ScriptData] = Future {
    val desc = commonApi.script(request.address.toAddress)
    ScriptData(desc.script.getOrElse(ByteStr.empty).toPBByteString, desc.scriptText.getOrElse(""), desc.complexity)
  }

  override def getActiveLeases(request: AccountRequest, responseObserver: StreamObserver[TransactionResponse]): Unit = {
    val transactions = commonApi.activeLeases(request.address.toAddress)

    val result = transactions.map { case (height, transaction) => TransactionResponse(transaction.id(), height, Some(transaction.toPB)) }
    responseObserver.completeWith(result)
  }

  override def getDataEntries(request: DataRequest, responseObserver: StreamObserver[DataEntryResponse]): Unit = {
    val stream = commonApi
      .dataStream(request.address.toAddress, key => request.key.isEmpty || key.matches(request.key))
      .map(de => DataEntryResponse(request.address, Some(PBTransactions.toPBDataEntry(de))))

    responseObserver.completeWith(stream)
  }
}
