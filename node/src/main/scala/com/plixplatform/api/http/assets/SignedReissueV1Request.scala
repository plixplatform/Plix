package com.plixplatform.api.http.assets

import com.plixplatform.account.PublicKey
import com.plixplatform.api.http.BroadcastRequest
import com.plixplatform.lang.ValidationError
import com.plixplatform.transaction.TransactionParsers.SignatureStringLength
import com.plixplatform.transaction.assets.ReissueTransactionV1
import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.{Format, Json}

object SignedReissueV1Request {
  implicit val assetReissueRequestReads: Format[SignedReissueV1Request] = Json.format
}

case class SignedReissueV1Request(@ApiModelProperty(value = "Base58 encoded Issuer public key", required = true)
                                  senderPublicKey: String,
                                  @ApiModelProperty(value = "Base58 encoded Asset ID", required = true)
                                  assetId: String,
                                  @ApiModelProperty(required = true, example = "1000000")
                                  quantity: Long,
                                  @ApiModelProperty(required = true)
                                  reissuable: Boolean,
                                  @ApiModelProperty(required = true)
                                  fee: Long,
                                  @ApiModelProperty(required = true)
                                  timestamp: Long,
                                  @ApiModelProperty(required = true)
                                  signature: String)
    extends BroadcastRequest {
  def toTx: Either[ValidationError, ReissueTransactionV1] =
    for {
      _sender    <- PublicKey.fromBase58String(senderPublicKey)
      _signature <- parseBase58(signature, "invalid.signature", SignatureStringLength)
      _assetId   <- parseBase58ToAsset(assetId)
      _t         <- ReissueTransactionV1.create(_sender, _assetId, quantity, reissuable, fee, timestamp, _signature)
    } yield _t
}
