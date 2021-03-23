package com.plixlatform.transaction

import com.plixlatform.TransactionGen
import com.plixlatform.account.{PublicKey, Address}
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.{Base58, EitherExt2}
import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.transaction.transfer._
import org.scalatest._
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import play.api.libs.json.Json

class TransferTransactionV2Specification extends PropSpec with PropertyChecks with Matchers with TransactionGen {

  property("VersionedTransferTransactionSpecification serialization roundtrip") {
    forAll(transferV2Gen) { tx: TransferTransactionV2 =>
      val recovered = TransferTransactionV2.parseBytes(tx.bytes()).get
      assertTxs(recovered, tx)
    }
  }

  property("VersionedTransferTransactionSpecification serialization from TypedTransaction") {
    forAll(transferV2Gen) { tx: TransferTransactionV2 =>
      val recovered = TransactionParsers.parseBytes(tx.bytes()).get
      assertTxs(recovered.asInstanceOf[TransferTransactionV2], tx)
    }
  }

  property("VersionedTransferTransactionSpecification id doesn't depend on proof") {
    forAll(accountGen, accountGen, proofsGen, proofsGen, bytes32gen) {
      case (acc1, acc2, proofs1, proofs2, attachment) =>
        val tx1 = TransferTransactionV2.create(Plix, acc2, acc2.toAddress, 1, 1, Plix, 1, attachment, proofs1).explicitGet()
        val tx2 = TransferTransactionV2.create(Plix, acc2, acc2.toAddress, 1, 1, Plix, 1, attachment, proofs2).explicitGet()
        tx1.id() shouldBe tx2.id()
    }
  }

  private def assertTxs(first: TransferTransactionV2, second: TransferTransactionV2): Unit = {
    first.sender.address shouldEqual second.sender.address
    first.timestamp shouldEqual second.timestamp
    first.fee shouldEqual second.fee
    first.amount shouldEqual second.amount
    first.recipient shouldEqual second.recipient
    first.version shouldEqual second.version
    first.assetId shouldEqual second.assetId
    first.feeAssetId shouldEqual second.feeAssetId
    first.proofs shouldEqual second.proofs
    first.bytes() shouldEqual second.bytes()
  }

  property("JSON format validation") {
    val js = Json.parse("""{
                       "type": 4,
                       "id": "6axEuUd3antXGd6Ynt8UMDF5H1VswsPPVrY6USXPn1i4",
                       "sender": "3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9",
                       "senderPublicKey": "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
                       "fee": 100000000,
                       "timestamp": 1526641218066,
                       "proofs": [
                       "4bfDaqBcnK3hT8ywFEFndxtS1DTSYfncUqd4s5Vyaa66PZHawtC73rDswUur6QZu5RpqM7L9NFgBHT1vhCoox4vi"
                       ],
                       "version": 2,
                       "recipient": "3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9",
                       "assetId": null,
                       "feeAsset": null,
                       "feeAssetId":null,
                       "amount": 100000000,
                       "attachment": "4t2Xazb2SX"}
    """)

    val tx = TransferTransactionV2
      .create(
        Plix,
        PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").explicitGet(),
        Address.fromString("3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9").explicitGet(),
        100000000,
        1526641218066L,
        Plix,
        100000000,
        Base58.tryDecodeWithLimit("4t2Xazb2SX").get,
        Proofs(Seq(ByteStr.decodeBase58("4bfDaqBcnK3hT8ywFEFndxtS1DTSYfncUqd4s5Vyaa66PZHawtC73rDswUur6QZu5RpqM7L9NFgBHT1vhCoox4vi").get))
      )
      .right
      .get

    tx.json() shouldEqual js
  }
}
