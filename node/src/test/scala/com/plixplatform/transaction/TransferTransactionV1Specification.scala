package com.plixlatform.transaction

import com.plixlatform.TransactionGen
import com.plixlatform.account.{PublicKey, Address}
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.{Base58, EitherExt2}
import com.plixlatform.state.diffs._
import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.transaction.transfer._
import org.scalatest._
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import play.api.libs.json.Json

class TransferTransactionV1Specification extends PropSpec with PropertyChecks with Matchers with TransactionGen {

  property("Transfer serialization roundtrip") {
    forAll(transferV1Gen) { transfer: TransferTransactionV1 =>
      val recovered = TransferTransactionV1.parseBytes(transfer.bytes()).get

      recovered.sender.address shouldEqual transfer.sender.address
      recovered.assetId shouldBe transfer.assetId
      recovered.feeAssetId shouldBe transfer.feeAssetId
      recovered.timestamp shouldEqual transfer.timestamp
      recovered.amount shouldEqual transfer.amount
      recovered.fee shouldEqual transfer.fee
      recovered.recipient.stringRepr shouldEqual transfer.recipient.stringRepr

      recovered.bytes() shouldEqual transfer.bytes()
    }
  }

  property("Transfer serialization from TypedTransaction") {
    forAll(transferV1Gen) { tx: TransferTransactionV1 =>
      val recovered = TransactionParsers.parseBytes(tx.bytes()).get
      recovered.bytes() shouldEqual tx.bytes()
    }
  }

  property("JSON format validation") {
    val js = Json.parse("""{
                        "type": 4,
                        "id": "GyvY8xLnzLKaBKNFVtGjgSL4AWfZrPbM5XtM7z7bfiF3",
                        "sender": "3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9",
                        "senderPublicKey": "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
                        "fee": 100000,
                        "timestamp": 1526552510868,
                        "signature": "eaV1i3hEiXyYQd6DQY7EnPg9XzpAvB9VA3bnpin2qJe4G36GZXaGnYKCgSf9xiQ61DcAwcBFzjSXh6FwCgazzFz",
                        "proofs": ["eaV1i3hEiXyYQd6DQY7EnPg9XzpAvB9VA3bnpin2qJe4G36GZXaGnYKCgSf9xiQ61DcAwcBFzjSXh6FwCgazzFz"],
                        "version": 1,
                        "recipient": "3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB",
                        "assetId": null,
                        "feeAsset":null,
                        "feeAssetId":null,
                        "amount": 1900000,
                        "attachment": "4t2Xazb2SX"
                        }
    """)

    val tx = TransferTransactionV1
      .create(
        Plix,
        PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").explicitGet(),
        Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
        1900000,
        1526552510868L,
        Plix,
        100000,
        Base58.tryDecodeWithLimit("4t2Xazb2SX").get,
        ByteStr.decodeBase58("eaV1i3hEiXyYQd6DQY7EnPg9XzpAvB9VA3bnpin2qJe4G36GZXaGnYKCgSf9xiQ61DcAwcBFzjSXh6FwCgazzFz").get
      )
      .right
      .get

    tx.json() shouldEqual js
  }

  property("negative") {
    for {
      (_, sender, recipient, amount, timestamp, _, feeAmount, attachment) <- transferParamGen
      sender                                                              <- accountGen
    } yield
      TransferTransactionV1.selfSigned(Plix, sender, recipient, amount, timestamp, Plix, feeAmount, attachment) should produce("insufficient fee")
  }
}
