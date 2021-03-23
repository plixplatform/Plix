package com.plixplatform.api.http.leasing

import com.plixplatform.account.{AddressOrAlias, PublicKey}
import com.plixplatform.api.http.BroadcastRequest
import com.plixplatform.lang.ValidationError
import com.plixplatform.transaction.TransactionParsers.SignatureStringLength
import com.plixplatform.transaction.lease.LeaseTransactionV1
import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.{Format, Json}

case class SignedLeaseV1Request(@ApiModelProperty(value = "Base58 encoded sender public key", required = true)
                                senderPublicKey: String,
                                @ApiModelProperty(required = true)
                                amount: Long,
                                @ApiModelProperty(required = true)
                                fee: Long,
                                @ApiModelProperty(value = "Recipient address", required = true)
                                recipient: String,
                                @ApiModelProperty(required = true)
                                timestamp: Long,
                                @ApiModelProperty(required = true)
                                signature: String)
    extends BroadcastRequest {
  def toTx: Either[ValidationError, LeaseTransactionV1] =
    for {
      _sender    <- PublicKey.fromBase58String(senderPublicKey)
      _signature <- parseBase58(signature, "invalid.signature", SignatureStringLength)
      _recipient <- AddressOrAlias.fromString(recipient)
      _t         <- LeaseTransactionV1.create(_sender, amount, fee, timestamp, _recipient, _signature)
    } yield _t
}

object SignedLeaseV1Request {
  implicit val broadcastLeaseRequestReadsFormat: Format[SignedLeaseV1Request] = Json.format
}
