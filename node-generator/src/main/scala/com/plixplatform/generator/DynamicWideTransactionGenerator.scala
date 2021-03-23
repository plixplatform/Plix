package com.plixlatform.generator

import java.util.concurrent.atomic.AtomicReference

import cats.Show
import com.plixlatform.account.KeyPair
import com.plixlatform.generator.DynamicWideTransactionGenerator.Settings
import com.plixlatform.generator.utils.Gen
import com.plixlatform.transaction.Transaction

class DynamicWideTransactionGenerator(settings: Settings, accounts: Seq[KeyPair]) extends TransactionGenerator {
  require(accounts.nonEmpty)

  private val nextTxsNumber = new AtomicReference[Double](settings.start)

  private val limitedRecipientGen = Gen.address(settings.limitDestAccounts)

  override def next(): Iterator[Transaction] = {
    val currTxsNumber = nextTxsNumber.getAndUpdate { x =>
      val newValue = x + settings.growAdder
      settings.maxTxsPerRequest.foldLeft(newValue)(Math.min(_, _))
    }.toInt

    Gen.txs(settings.minFee, settings.maxFee, accounts, limitedRecipientGen).take(currTxsNumber)
  }

}

object DynamicWideTransactionGenerator {

  case class Settings(start: Int, growAdder: Double, maxTxsPerRequest: Option[Int], limitDestAccounts: Option[Int], minFee: Long, maxFee: Long) {
    require(start >= 1)
  }

  object Settings {
    implicit val toPrintable: Show[Settings] = { x =>
      import x._
      s"""txs at start: $start
         |grow adder: $growAdder
         |max txs: $maxTxsPerRequest
         |limit destination accounts: $limitDestAccounts
         |min fee: $minFee
         |max fee: $maxFee""".stripMargin
    }
  }

}
