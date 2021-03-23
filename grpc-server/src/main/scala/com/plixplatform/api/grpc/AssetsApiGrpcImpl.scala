package com.plixlatform.api.grpc
import com.google.protobuf.ByteString
import com.plixlatform.api.common.CommonAssetsApi
import com.plixlatform.state.Blockchain
import com.plixlatform.transaction.Asset.IssuedAsset
import com.plixlatform.transaction.TxValidationError.GenericError
import monix.execution.Scheduler

import scala.concurrent.Future

class AssetsApiGrpcImpl(blockchain: Blockchain)(implicit sc: Scheduler) extends AssetsApiGrpc.AssetsApi {
  private[this] val commonApi = new CommonAssetsApi(blockchain)

  override def getInfo(request: AssetRequest): Future[AssetInfoResponse] = Future {
    val info = commonApi.fullInfo(IssuedAsset(request.assetId))
      .getOrElse(throw GenericError("Asset not found"))

    AssetInfoResponse(
      info.description.issuer,
      ByteString.copyFrom(info.description.name),
      ByteString.copyFrom(info.description.description),
      info.description.decimals,
      info.description.reissuable,
      info.description.totalVolume.longValue(),
      info.description.script.map(script => ScriptData(
        script.bytes().toPBByteString,
        script.expr.toString,
        script.complexity
      )),
      info.description.sponsorship,
      Some(info.issueTransaction.toPB),
      info.sponsorBalance.getOrElse(0)
    )
  }
}