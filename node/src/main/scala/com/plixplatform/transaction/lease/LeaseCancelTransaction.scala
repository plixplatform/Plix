package com.plixplatform.transaction.lease

import com.google.common.primitives.{Bytes, Longs}
import com.plixplatform.common.state.ByteStr
import com.plixplatform.crypto
import com.plixplatform.lang.ValidationError
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.TxValidationError._
import com.plixplatform.transaction.{Asset, ProvenTransaction, VersionedTransaction}
import monix.eval.Coeval
import play.api.libs.json.{JsObject, Json}

trait LeaseCancelTransaction extends ProvenTransaction with VersionedTransaction {
  def chainByte: Option[Byte]
  def leaseId: ByteStr
  def fee: Long
  override val assetFee: (Asset, Long) = (Plix, fee)

  override val json: Coeval[JsObject] = Coeval.evalOnce(
    jsonBase() ++ Json.obj(
      "chainId"   -> chainByte,
      "version"   -> version,
      "fee"       -> fee,
      "timestamp" -> timestamp,
      "leaseId"   -> leaseId.base58
    ))
  protected val bytesBase: Coeval[Array[Byte]] =
    Coeval.evalOnce(Bytes.concat(sender, Longs.toByteArray(fee), Longs.toByteArray(timestamp), leaseId.arr))

}

object LeaseCancelTransaction {

  val typeId: Byte = 9

  def validateLeaseCancelParams(tx: LeaseCancelTransaction): Either[ValidationError, Unit] = {
    validateLeaseCancelParams(tx.leaseId, tx.fee)
  }

  def validateLeaseCancelParams(leaseId: ByteStr, fee: Long): Either[ValidationError, Unit] =
    if (leaseId.arr.length != crypto.DigestSize) {
      Left(GenericError("Lease transaction id is invalid"))
    } else if (fee <= 0) {
      Left(InsufficientFee())
    } else Right(())
}
