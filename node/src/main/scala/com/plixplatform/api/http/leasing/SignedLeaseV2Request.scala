package com.plixplatform.api.http.leasing

import cats.implicits._
import com.plixplatform.account.{AddressOrAlias, PublicKey}
import com.plixplatform.api.http.BroadcastRequest
import com.plixplatform.lang.ValidationError
import com.plixplatform.transaction.lease.LeaseTransactionV2
import com.plixplatform.transaction.Proofs
import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.{Format, Json}

case class SignedLeaseV2Request(@ApiModelProperty(value = "Base58 encoded sender public key", required = true)
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
                                proofs: List[String])
    extends BroadcastRequest {
  def toTx: Either[ValidationError, LeaseTransactionV2] =
    for {
      _sender     <- PublicKey.fromBase58String(senderPublicKey)
      _proofBytes <- proofs.traverse(s => parseBase58(s, "invalid proof", Proofs.MaxProofStringSize))
      _proofs     <- Proofs.create(_proofBytes)
      _recipient  <- AddressOrAlias.fromString(recipient)
      _t          <- LeaseTransactionV2.create(_sender, amount, fee, timestamp, _recipient, _proofs)
    } yield _t
}

object SignedLeaseV2Request {
  implicit val broadcastLeaseRequestReadsFormat: Format[SignedLeaseV2Request] = Json.format
}
