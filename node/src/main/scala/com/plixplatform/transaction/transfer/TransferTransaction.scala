package com.plixlatform.transaction.transfer

import cats.implicits._
import com.google.common.primitives.{Bytes, Longs}
import com.plixlatform.account.AddressOrAlias
import com.plixlatform.common.utils.Base58
import com.plixlatform.lang.ValidationError
import com.plixlatform.serialization.Deser
import com.plixlatform.transaction._
import com.plixlatform.transaction.Asset.{IssuedAsset, Plix}
import com.plixlatform.transaction.validation._
import com.plixlatform.utils.base58Length
import monix.eval.Coeval
import play.api.libs.json.{JsObject, Json}

trait TransferTransaction extends ProvenTransaction with VersionedTransaction {
  def assetId: Asset
  def recipient: AddressOrAlias
  def amount: Long
  def feeAssetId: Asset
  def fee: Long
  def attachment: Array[Byte]
  def version: Byte

  override val assetFee: (Asset, Long) = (feeAssetId, fee)

  override final val json: Coeval[JsObject] = Coeval.evalOnce(
    jsonBase() ++ Json.obj(
      "version"    -> version,
      "recipient"  -> recipient.stringRepr,
      "assetId"    -> assetId.maybeBase58Repr,
      "feeAsset"   -> feeAssetId.maybeBase58Repr, // legacy v0.11.1 compat
      "amount"     -> amount,
      "attachment" -> Base58.encode(attachment)
    ))

  final protected val bytesBase: Coeval[Array[Byte]] = Coeval.evalOnce {
    val timestampBytes  = Longs.toByteArray(timestamp)
    val assetIdBytes    = assetId.byteRepr
    val feeAssetIdBytes = feeAssetId.byteRepr
    val amountBytes     = Longs.toByteArray(amount)
    val feeBytes        = Longs.toByteArray(fee)

    Bytes.concat(
      sender,
      assetIdBytes,
      feeAssetIdBytes,
      timestampBytes,
      amountBytes,
      feeBytes,
      recipient.bytes.arr,
      Deser.serializeArray(attachment)
    )
  }
  override def checkedAssets(): Seq[IssuedAsset] = assetId match {
    case Plix => Seq()
    case a: IssuedAsset => Seq(a)
  }
}

object TransferTransaction {

  val typeId: Byte = 4

  val MaxAttachmentSize            = 140
  val MaxAttachmentStringSize: Int = base58Length(MaxAttachmentSize)

  def validate(tx: TransferTransaction): Either[ValidationError, Unit] = {
    validate(tx.amount, tx.assetId, tx.fee, tx.feeAssetId, tx.attachment)
  }

  def validate(amt: Long, maybeAmtAsset: Asset, feeAmt: Long, maybeFeeAsset: Asset, attachment: Array[Byte]): Either[ValidationError, Unit] = {
    (
      validateAmount(amt, maybeAmtAsset.maybeBase58Repr.getOrElse("plix")),
      validateFee(feeAmt),
      validateAttachment(attachment)
    ).mapN { case _ => () }
      .toEither
      .leftMap(_.head)
  }
}
