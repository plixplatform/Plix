package com.plixplatform.it.sync.transactions

import com.typesafe.config.Config
import com.plixplatform.account.Address
import com.plixplatform.common.utils._
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.api.TransactionInfo
import com.plixplatform.it.transactions.NodesFromDocker
import com.plixplatform.it.{Node, NodeConfigs, ReportingTestName}
import com.plixplatform.transaction.Asset
import com.plixplatform.transaction.transfer.{TransferTransaction, TransferTransactionV1}
import org.scalatest.{CancelAfterFailure, FreeSpec, Matchers}
import play.api.libs.json.JsNumber

import scala.concurrent.duration._

class TransactionAPISuite extends FreeSpec with NodesFromDocker with Matchers with ReportingTestName with CancelAfterFailure {

  override def nodeConfigs: Seq[Config] =
    NodeConfigs.newBuilder
      .overrideBase(_.quorum(0))
      .overrideBase(_.raw("plix.rest-api.transactions-by-address-limit=10"))
      .withDefault(1)
      .withSpecial(1, _.nonMiner)
      .buildNonConflicting()

  val sender: Node       = nodes.head
  val recipient: Address = Address.fromString(sender.createAddress()).explicitGet()

  val Plix: Long = 100000000L

  val AMT: Long = 1 * Plix
  val FEE: Long = (0.001 * Plix).toLong

  val transactions: List[TransferTransaction] =
    (for (i <- 0 to 30) yield {
      TransferTransactionV1
        .selfSigned(
          Asset.Plix,
          sender.privateKey,
          recipient,
          AMT,
          System.currentTimeMillis() + i,
          Asset.Plix,
          FEE + i * 100,
          Array.emptyByteArray
        )
        .explicitGet()
    }).toList

  val transactionIds = transactions.map(_.id().base58)

  "should accept transactions" in {
    transactions.foreach { tx =>
      sender.broadcastRequest(tx.json() + ("type" -> JsNumber(tx.builder.typeId.toInt)))
    }

    val h = sender.height

    sender.waitForHeight(h + 3, 2.minutes)
  }

  "should return correct N txs on request without `after`" in {

    def checkForLimit(limit: Int): Unit = {
      val expected =
        transactionIds
          .take(limit)

      val received =
        sender
          .transactionsByAddress(recipient.address, limit)
          .map(_.id)

      expected shouldEqual received
    }

    for (limit <- 2 to 10 by 1) {
      checkForLimit(limit)
    }
  }

  "should return correct N txs on request with `after`" in {

    def checkForLimit(limit: Int): Unit = {
      val expected =
        transactionIds
          .slice(limit, limit + limit)

      val afterParam =
        transactions
          .drop(limit - 1)
          .head
          .id()
          .base58

      val received =
        sender
          .transactionsByAddress(recipient.address, limit, afterParam)
          .map(_.id)

      expected shouldEqual received
    }

    for (limit <- 2 to 10 by 1) {
      checkForLimit(limit)
    }
  }

  "should return all transactions" in {
    def checkForLimit(limit: Int): Unit = {
      val received =
        loadAll(sender, recipient.address, limit, None, Nil)
          .map(_.id)

      received shouldEqual transactionIds
    }

    for (limit <- 2 to 10 by 1) {
      checkForLimit(limit)
    }
  }

  def loadAll(node: Node, address: String, limit: Int, maybeAfter: Option[String], acc: List[TransactionInfo]): List[TransactionInfo] = {
    val txs = maybeAfter match {
      case None         => node.transactionsByAddress(address, limit).toList
      case Some(lastId) => node.transactionsByAddress(address, limit, lastId).toList
    }

    txs.lastOption match {
      case None     => acc ++ txs
      case Some(tx) => loadAll(node, address, limit, Some(tx.id), acc ++ txs)
    }
  }
}
