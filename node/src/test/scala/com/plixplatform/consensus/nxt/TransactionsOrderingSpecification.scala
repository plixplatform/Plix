package com.plixlatform.consensus.nxt

import com.plixlatform.account.{KeyPair, Address}
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.consensus.TransactionsOrdering
import com.plixlatform.transaction.Asset
import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.transaction.transfer._
import org.scalatest.{Assertions, Matchers, PropSpec}

import scala.util.Random

class TransactionsOrderingSpecification extends PropSpec with Assertions with Matchers {

  property("TransactionsOrdering.InBlock should sort correctly") {
    val correctSeq = Seq(
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          1,
          Plix,
          125L,
          Array.empty
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(Plix,
                    KeyPair(Array.fill(32)(0: Byte)),
                    Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
                    100000,
                    2,
                    Plix,
                    124L,
                    Array.empty)
        .right
        .get,
      TransferTransactionV1
        .selfSigned(Plix,
                    KeyPair(Array.fill(32)(0: Byte)),
                    Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
                    100000,
                    1,
                    Plix,
                    124L,
                    Array.empty)
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          2,
          Asset.fromCompatId(Some(ByteStr.empty)),
          124L,
          Array.empty
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          1,
          Asset.fromCompatId(Some(ByteStr.empty)),
          124L,
          Array.empty
        )
        .right
        .get
    )

    val sorted = Random.shuffle(correctSeq).sorted(TransactionsOrdering.InBlock)

    sorted shouldBe correctSeq
  }

  property("TransactionsOrdering.InUTXPool should sort correctly") {
    val correctSeq = Seq(
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          1,
          Plix,
          124L,
          Array.empty
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          1,
          Plix,
          123L,
          Array.empty
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          2,
          Plix,
          123L,
          Array.empty
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          1,
          Asset.fromCompatId(Some(ByteStr.empty)),
          124L,
          Array.empty
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          2,
          Asset.fromCompatId(Some(ByteStr.empty)),
          124L,
          Array.empty
        )
        .right
        .get
    )

    val sorted = Random.shuffle(correctSeq).sorted(TransactionsOrdering.InUTXPool)

    sorted shouldBe correctSeq
  }

  property("TransactionsOrdering.InBlock should sort txs by decreasing block timestamp") {
    val correctSeq = Seq(
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          124L,
          Plix,
          1,
          Array()
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          123L,
          Plix,
          1,
          Array()
        )
        .right
        .get
    )

    Random.shuffle(correctSeq).sorted(TransactionsOrdering.InBlock) shouldBe correctSeq
  }

  property("TransactionsOrdering.InUTXPool should sort txs by ascending block timestamp") {
    val correctSeq = Seq(
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          123L,
          Plix,
          1,
          Array()
        )
        .right
        .get,
      TransferTransactionV1
        .selfSigned(
          Plix,
          KeyPair(Array.fill(32)(0: Byte)),
          Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").explicitGet(),
          100000,
          124L,
          Plix,
          1,
          Array()
        )
        .right
        .get
    )
    Random.shuffle(correctSeq).sorted(TransactionsOrdering.InUTXPool) shouldBe correctSeq
  }
}
