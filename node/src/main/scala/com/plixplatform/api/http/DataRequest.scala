package com.plixplatform.api.http

import cats.implicits._
import com.plixplatform.account.PublicKey
import com.plixplatform.lang.ValidationError
import com.plixplatform.state.DataEntry
import com.plixplatform.transaction.{DataTransaction, Proofs}
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import play.api.libs.json.Json

import scala.annotation.meta.field

object DataRequest {
  implicit val unsignedDataRequestReads = Json.reads[DataRequest]
  implicit val signedDataRequestReads   = Json.reads[SignedDataRequest]
}

case class DataRequest(sender: String,
                       @(ApiModelProperty @field)(required = true) data: List[DataEntry[_]],
                       @(ApiModelProperty @field)(required = true, value = "1000") fee: Long,
                       timestamp: Option[Long] = None)

@ApiModel(value = "Signed Data transaction")
case class SignedDataRequest(@(ApiModelProperty @field)(value = "Base58 encoded sender public key", required = true)
                             senderPublicKey: String,
                             @(ApiModelProperty @field)(value = "Data to put into blockchain", required = true)
                             data: List[DataEntry[_]],
                             @(ApiModelProperty @field)(required = true)
                             fee: Long,
                             @(ApiModelProperty @field)(required = true, value = "1000")
                             timestamp: Long,
                             @(ApiModelProperty @field)(required = true)
                             proofs: List[String])
    extends BroadcastRequest {
  def toTx: Either[ValidationError, DataTransaction] =
    for {
      _sender     <- PublicKey.fromBase58String(senderPublicKey)
      _proofBytes <- proofs.traverse(s => parseBase58(s, "invalid proof", Proofs.MaxProofStringSize))
      _proofs     <- Proofs.create(_proofBytes)
      t           <- DataTransaction.create(_sender, data, fee, timestamp, _proofs)
    } yield t
}
