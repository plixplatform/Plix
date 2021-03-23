package com.plixlatform.transaction

import com.plixlatform.TransactionGen
import com.plixlatform.account.{PublicKey, Address}
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.transaction.lease.{LeaseTransaction, LeaseTransactionV1, LeaseTransactionV2}
import org.scalatest._
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import play.api.libs.json.Json

class LeaseTransactionSpecification extends PropSpec with PropertyChecks with Matchers with TransactionGen {

  property("Lease transaction serialization roundtrip") {
    forAll(leaseGen) { tx: LeaseTransaction =>
      val recovered = tx.builder.parseBytes(tx.bytes()).get.asInstanceOf[LeaseTransaction]
      assertTxs(recovered, tx)
    }
  }

  property("Lease transaction from TransactionParser") {
    forAll(leaseGen) { tx: LeaseTransaction =>
      val recovered = TransactionParsers.parseBytes(tx.bytes()).get
      assertTxs(recovered.asInstanceOf[LeaseTransaction], tx)
    }
  }

  private def assertTxs(first: LeaseTransaction, second: LeaseTransaction): Unit = {
    first.sender.address shouldEqual second.sender.address
    first.recipient.stringRepr shouldEqual second.recipient.stringRepr
    first.amount shouldEqual second.amount
    first.fee shouldEqual second.fee
    first.proofs shouldEqual second.proofs
    first.bytes() shouldEqual second.bytes()
  }

  property("JSON format validation for LeaseTransactionV1") {
    val js = Json.parse("""{
                       "type": 8,
                       "id": "6GVDpoBoVtC6FDNjnDB5ZjtWdYJMvuNsPfXyhbDbCMcy",
                       "sender": "3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9",
                       "senderPublicKey": "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
                       "fee": 1000000,
                       "feeAssetId": null,
                       "timestamp": 1526646300260,
                       "signature": "iy3TmfbFds7pc9cDDqfjEJhfhVyNtm3GcxoVz8L3kJFvgRPUmiqqKLMeJGYyN12AhaQ6HvE7aF1tFgaAoCCgNJJ",
                       "proofs": ["iy3TmfbFds7pc9cDDqfjEJhfhVyNtm3GcxoVz8L3kJFvgRPUmiqqKLMeJGYyN12AhaQ6HvE7aF1tFgaAoCCgNJJ"],
                       "version": 1,
                       "amount": 10000000,
                       "recipient": "3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB"
                       }
    """)

    val tx = LeaseTransactionV1
      .create(
        PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").explicitGet(),
        10000000,
        1000000,
        1526646300260L,
        Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
        ByteStr.decodeBase58("iy3TmfbFds7pc9cDDqfjEJhfhVyNtm3GcxoVz8L3kJFvgRPUmiqqKLMeJGYyN12AhaQ6HvE7aF1tFgaAoCCgNJJ").get
      )
      .right
      .get

    js shouldEqual tx.json()
  }

  property("JSON format validation for LeaseTransactionV2") {
    val js = Json.parse("""{
                        "type": 8,
                        "id": "KVJUzauo8X3tjFq4PSjgby7zyrnwcP4qtBwpEaDnqx6",
                        "sender": "3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9",
                        "senderPublicKey": "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
                        "fee": 1000000,
                        "feeAssetId": null,
                        "timestamp": 1526646497465,
                        "proofs": [
                        "5Fr3yLwvfKGDsFLi8A8JbHqToHDojrPbdEGx9mrwbeVWWoiDY5pRqS3rcX1rXC9ud52vuxVdBmGyGk5krcgwFu9q"
                        ],
                        "version": 2,
                        "amount": 10000000,
                        "recipient": "3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB"
                       }
    """)

    val tx = LeaseTransactionV2
      .create(
        PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").explicitGet(),
        10000000,
        1000000,
        1526646497465L,
        Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
        Proofs(Seq(ByteStr.decodeBase58("5Fr3yLwvfKGDsFLi8A8JbHqToHDojrPbdEGx9mrwbeVWWoiDY5pRqS3rcX1rXC9ud52vuxVdBmGyGk5krcgwFu9q").get))
      )
      .right
      .get

    js shouldEqual tx.json()
  }

  property("forbid assetId in LeaseTransactionV2") {
    val leaseV2Gen      = leaseGen.filter(_.version == 2)
    val assetIdBytesGen = bytes32gen
    forAll(leaseV2Gen, assetIdBytesGen) { (tx, assetId) =>
      val bytes = tx.bytes()
      // hack in an assetId
      bytes(3) = 1: Byte
      val bytesWithAssetId = bytes.take(4) ++ assetId ++ bytes.drop(4)
      val parsed           = tx.builder.parseBytes(bytesWithAssetId)
      parsed.isFailure shouldBe true
      parsed.failed.get.getMessage.contains("Leasing assets is not supported yet") shouldBe true
    }
  }
}
