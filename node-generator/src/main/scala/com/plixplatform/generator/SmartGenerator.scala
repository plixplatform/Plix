package com.plixplatform.generator

import java.util.concurrent.ThreadLocalRandom

import cats.Show
import com.plixplatform.account.KeyPair
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.generator.utils.Gen
import com.plixplatform.it.util._
import com.plixplatform.lang.script.Script
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.assets.exchange.{AssetPair, ExchangeTransactionV2, OrderV2}
import com.plixplatform.transaction.smart.SetScriptTransaction
import com.plixplatform.transaction.transfer.TransferTransactionV2
import com.plixplatform.transaction.{Asset, Transaction}

import scala.concurrent.duration._

class SmartGenerator(settings: SmartGenerator.Settings, val accounts: Seq[KeyPair]) extends TransactionGenerator {
  private def r                                   = ThreadLocalRandom.current
  private def randomFrom[T](c: Seq[T]): Option[T] = if (c.nonEmpty) Some(c(r.nextInt(c.size))) else None

  override def next(): Iterator[Transaction] = {
    generate(settings).toIterator
  }

  private def generate(settings: SmartGenerator.Settings): Seq[Transaction] = {
    val bank = randomFrom(accounts).get

    val fee = 0.005.plix

    val script: Script = Gen.script(settings.complexity)

    val setScripts = Range(0, settings.scripts) flatMap (_ =>
      accounts.map { i =>
        SetScriptTransaction.selfSigned(i, Some(script), 1.plix, System.currentTimeMillis()).explicitGet()
      })

    val now = System.currentTimeMillis()
    val txs = Range(0, settings.transfers).map { i =>
      TransferTransactionV2
        .selfSigned(Plix, bank, bank, 1.plix - 2 * fee, now + i, Plix, fee, Array.emptyByteArray)
        .explicitGet()
    }

    val extxs = Range(0, settings.exchange).map { i =>
      val ts = now + i

      val matcher         = randomFrom(accounts).get
      val seller          = randomFrom(accounts).get
      val buyer           = randomFrom(accounts).get
      val asset           = randomFrom(settings.assets.toSeq)
      val tradeAssetIssue = ByteStr.decodeBase58(asset.get).toOption
      val pair            = AssetPair(Plix, Asset.fromCompatId(tradeAssetIssue))
      val sellOrder       = OrderV2.sell(seller, matcher, pair, 100000000L, 1, ts, ts + 30.days.toMillis, 0.003.plix)
      val buyOrder        = OrderV2.buy(buyer, matcher, pair, 100000000L, 1, ts, ts + 1.day.toMillis, 0.003.plix)

      ExchangeTransactionV2.create(matcher, buyOrder, sellOrder, 100000000, 1, 0.003.plix, 0.003.plix, 0.011.plix, ts).explicitGet()
    }

    setScripts ++ txs ++ extxs
  }

}

object SmartGenerator {
  final case class Settings(scripts: Int, transfers: Int, complexity: Boolean, exchange: Int, assets: Set[String]) {
    require(scripts >= 0)
    require(transfers >= 0)
    require(exchange >= 0)
  }

  object Settings {
    implicit val toPrintable: Show[Settings] = { x =>
      import x._
      s"""
         | set-scripts = $scripts
         | transfers = $transfers
         | complexity = $complexity
         | exchange = $exchange
         | assets = $assets
      """.stripMargin
    }

  }
}
