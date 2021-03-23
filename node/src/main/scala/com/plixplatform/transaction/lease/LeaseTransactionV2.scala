package com.plixlatform.transaction.lease

import cats.implicits._
import com.google.common.primitives.Bytes
import com.plixlatform.account.{AddressOrAlias, KeyPair, PrivateKey, PublicKey}
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.crypto
import com.plixlatform.lang.ValidationError
import com.plixlatform.serialization.Deser
import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.transaction.TxValidationError._
import com.plixlatform.transaction._
import com.plixlatform.transaction.description._
import monix.eval.Coeval

import scala.util.{Either, Try}

case class LeaseTransactionV2 private (sender: PublicKey, amount: Long, fee: Long, timestamp: Long, recipient: AddressOrAlias, proofs: Proofs)
    extends LeaseTransaction
    with FastHashId {

  override val builder: TransactionParser = LeaseTransactionV2

  val bodyBytes: Coeval[Array[Byte]] = Coeval.evalOnce {
    val assetId: Asset = Plix // placeholder for future enhancement
    Bytes.concat(Array(builder.typeId, version), assetId.byteRepr, bytesBase())
  }

  override val bytes: Coeval[Array[Byte]] = Coeval.evalOnce(Bytes.concat(Array(0: Byte), bodyBytes(), proofs.bytes()))

  override def version: Byte = 2
}

object LeaseTransactionV2 extends TransactionParserFor[LeaseTransactionV2] with TransactionParser.MultipleVersions {

  override def supportedVersions: Set[Byte] = Set(2)

  override val typeId: Byte = LeaseTransaction.typeId

  override protected def parseTail(bytes: Array[Byte]): Try[TransactionT] = {
    byteTailDescription.deserializeFromByteArray(bytes).flatMap { tx =>
      val (assetIdOpt, _) = Deser.parseByteArrayOption(bytes, 0, AssetIdLength)
      Either
        .cond(assetIdOpt.isEmpty, (), GenericError("Leasing assets is not supported yet"))
        .flatMap(_ => LeaseTransaction.validateLeaseParams(tx))
        .map(_ => tx)
        .foldToTry
    }
  }

  def create(sender: PublicKey,
             amount: Long,
             fee: Long,
             timestamp: Long,
             recipient: AddressOrAlias,
             proofs: Proofs): Either[ValidationError, TransactionT] = {
    for {
      _ <- LeaseTransaction.validateLeaseParams(amount, fee, recipient, sender)
    } yield LeaseTransactionV2(sender, amount, fee, timestamp, recipient, proofs)
  }

  def signed(sender: PublicKey,
             amount: Long,
             fee: Long,
             timestamp: Long,
             recipient: AddressOrAlias,
             signer: PrivateKey): Either[ValidationError, TransactionT] = {
    for {
      unverified <- create(sender, amount, fee, timestamp, recipient, Proofs.empty)
      proofs     <- Proofs.create(Seq(ByteStr(crypto.sign(signer, unverified.bodyBytes()))))
    } yield unverified.copy(proofs = proofs)
  }

  def selfSigned(sender: KeyPair, amount: Long, fee: Long, timestamp: Long, recipient: AddressOrAlias): Either[ValidationError, TransactionT] = {
    signed(sender, amount, fee, timestamp, recipient, sender)
  }

  val byteTailDescription: ByteEntity[LeaseTransactionV2] = {
    (
      OptionBytes(tailIndex(1), "Leasing asset", AssetIdBytes(tailIndex(1), "Leasing asset"), "flag (1 - asset, 0 - Plix)"),
      PublicKeyBytes(tailIndex(2), "Sender's public key"),
      AddressOrAliasBytes(tailIndex(3), "Recipient"),
      LongBytes(tailIndex(4), "Amount"),
      LongBytes(tailIndex(5), "Fee"),
      LongBytes(tailIndex(6), "Timestamp"),
      ProofsBytes(tailIndex(7))
    ) mapN {
      case (_, senderPublicKey, recipient, amount, fee, timestamp, proofs) =>
        LeaseTransactionV2(
          sender = senderPublicKey,
          amount = amount,
          fee = fee,
          timestamp = timestamp,
          recipient = recipient,
          proofs = proofs
        )
    }
  }
}
