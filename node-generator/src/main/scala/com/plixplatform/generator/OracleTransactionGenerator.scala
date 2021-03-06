package com.plixplatform.generator

import cats.Show
import com.plixplatform.account.KeyPair
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.generator.OracleTransactionGenerator.Settings
import com.plixplatform.generator.utils.Gen
import com.plixplatform.it.util._
import com.plixplatform.state._
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.smart.SetScriptTransaction
import com.plixplatform.transaction.transfer.TransferTransactionV2
import com.plixplatform.transaction.{DataTransaction, Transaction}

class OracleTransactionGenerator(settings: Settings, val accounts: Seq[KeyPair]) extends TransactionGenerator {
  override def next(): Iterator[Transaction] = generate(settings).toIterator

  def generate(settings: Settings): Seq[Transaction] = {
    val oracle = accounts.last

    val scriptedAccount = accounts.head

    val script = Gen.oracleScript(oracle, settings.requiredData)

    val enoughFee = 0.005.plix

    val setScript: Transaction =
      SetScriptTransaction
        .selfSigned(scriptedAccount, Some(script), enoughFee, System.currentTimeMillis())
        .explicitGet()

    val setDataTx: Transaction = DataTransaction
      .selfSigned(oracle, settings.requiredData.toList, enoughFee, System.currentTimeMillis())
      .explicitGet()

    val now = System.currentTimeMillis()
    val transactions: List[Transaction] = (1 to settings.transactions).map { i =>
      TransferTransactionV2
        .selfSigned(Plix, scriptedAccount, oracle, 1.plix, now + i, Plix, enoughFee, Array.emptyByteArray)
        .explicitGet()
    }.toList

    setScript +: setDataTx +: transactions
  }
}

object OracleTransactionGenerator {
  final case class Settings(transactions: Int, requiredData: Set[DataEntry[_]])

  object Settings {
    implicit val toPrintable: Show[Settings] = { x =>
      s"Transactions: ${x.transactions}\n" +
        s"DataEntries: ${x.requiredData}\n"
    }
  }
}
