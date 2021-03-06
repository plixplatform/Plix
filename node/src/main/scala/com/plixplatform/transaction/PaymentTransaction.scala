package com.plixplatform.transaction

import cats.implicits._
import com.google.common.primitives.{Bytes, Ints, Longs}
import com.plixplatform.account.{Address, KeyPair, PublicKey}
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.crypto
import com.plixplatform.crypto._
import com.plixplatform.lang.ValidationError
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.TransactionParsers._
import com.plixplatform.transaction.description._
import monix.eval.Coeval
import play.api.libs.json.{JsObject, Json}

import scala.util.Try

case class PaymentTransaction private (sender: PublicKey, recipient: Address, amount: Long, fee: Long, timestamp: Long, signature: ByteStr)
    extends SignedTransaction {

  override val builder: TransactionParser = PaymentTransaction
  override val assetFee: (Asset, Long)    = (Plix, fee)
  override val id: Coeval[ByteStr]        = Coeval.evalOnce(signature)
  override val json: Coeval[JsObject]     = Coeval.evalOnce(jsonBase() ++ Json.obj("recipient" -> recipient.address, "amount" -> amount))

  private val hashBytes: Coeval[Array[Byte]] = Coeval.evalOnce(
    Bytes.concat(Array(builder.typeId), Longs.toByteArray(timestamp), sender, recipient.bytes.arr, Longs.toByteArray(amount), Longs.toByteArray(fee)))

  override val bodyBytes: Coeval[Array[Byte]] = Coeval.evalOnce(
    Bytes.concat(Ints.toByteArray(builder.typeId),
                 Longs.toByteArray(timestamp),
                 sender,
                 recipient.bytes.arr,
                 Longs.toByteArray(amount),
                 Longs.toByteArray(fee)))

  val hash: Coeval[Array[Byte]] = Coeval.evalOnce(crypto.fastHash(hashBytes()))

  override val bytes: Coeval[Array[Byte]] = Coeval.evalOnce(Bytes.concat(hashBytes(), signature.arr))
}

object PaymentTransaction extends TransactionParserFor[PaymentTransaction] with TransactionParser.HardcodedVersion1 {

  override val typeId: Byte = 2

  val RecipientLength: Int = Address.AddressLength

  private val SenderLength = 32
  private val FeeLength    = 8
  private val BaseLength   = TimestampLength + SenderLength + RecipientLength + AmountLength + FeeLength + SignatureLength

  def create(sender: KeyPair, recipient: Address, amount: Long, fee: Long, timestamp: Long): Either[ValidationError, TransactionT] = {
    create(sender, recipient, amount, fee, timestamp, ByteStr.empty).right.map(unsigned => {
      unsigned.copy(signature = ByteStr(crypto.sign(sender, unsigned.bodyBytes())))
    })
  }

  def create(sender: PublicKey,
             recipient: Address,
             amount: Long,
             fee: Long,
             timestamp: Long,
             signature: ByteStr): Either[ValidationError, TransactionT] = {
    if (amount <= 0) {
      Left(TxValidationError.NonPositiveAmount(amount, "plix")) //CHECK IF AMOUNT IS POSITIVE
    } else if (fee <= 0) {
      Left(TxValidationError.InsufficientFee()) //CHECK IF FEE IS POSITIVE
    } else if (Try(Math.addExact(amount, fee)).isFailure) {
      Left(TxValidationError.OverflowError) // CHECK THAT fee+amount won't overflow Long
    } else {
      Right(PaymentTransaction(sender, recipient, amount, fee, timestamp, signature))
    }
  }

  override protected def parseTail(bytes: Array[Byte]): Try[TransactionT] = {
    Try {

      require(bytes.length >= BaseLength, "Data does not match base length")

      byteTailDescription.deserializeFromByteArray(bytes).flatMap { tx =>
        (
          if (tx.amount <= 0) {
            Left(TxValidationError.NonPositiveAmount(tx.amount, "plix")) //CHECK IF AMOUNT IS POSITIVE
          } else if (tx.fee <= 0) {
            Left(TxValidationError.InsufficientFee) //CHECK IF FEE IS POSITIVE
          } else if (Try(Math.addExact(tx.amount, tx.fee)).isFailure) {
            Left(TxValidationError.OverflowError) // CHECK THAT fee+amount won't overflow Long
          } else {
            Right(tx)
          }
        ).foldToTry
      }
    }.flatten
  }

  val byteTailDescription: ByteEntity[PaymentTransaction] = {
    (
      LongBytes(tailIndex(1), "Timestamp"),
      PublicKeyBytes(tailIndex(2), "Sender's public key"),
      AddressBytes(tailIndex(3), "Recipient's address"),
      LongBytes(tailIndex(4), "Amount"),
      LongBytes(tailIndex(5), "Fee"),
      SignatureBytes(tailIndex(6), "Signature")
    ) mapN {
      case (timestamp, senderPublicKey, recipient, amount, fee, signature) =>
        PaymentTransaction(
          sender = senderPublicKey,
          recipient = recipient,
          amount = amount,
          fee = fee,
          timestamp = timestamp,
          signature = signature
        )
    }
  }
}
