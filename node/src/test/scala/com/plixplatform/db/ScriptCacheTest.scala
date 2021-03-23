package com.plixplatform.db

import com.typesafe.config.ConfigFactory
import com.plixplatform.account.KeyPair
import com.plixplatform.block.Block
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.database.LevelDBWriter
import com.plixplatform.lagonaki.mocks.TestBlock
import com.plixplatform.lang.script.Script
import com.plixplatform.settings.{TestFunctionalitySettings, PlixSettings, loadConfig}
import com.plixplatform.state.{BlockchainUpdaterImpl, _}
import com.plixplatform.transaction.smart.SetScriptTransaction
import com.plixplatform.transaction.smart.script.ScriptCompiler
import com.plixplatform.transaction.{BlockchainUpdater, GenesisTransaction}
import com.plixplatform.utils.Time
import com.plixplatform.{TransactionGen, WithDB}
import org.scalacheck.Gen
import org.scalatest.{FreeSpec, Matchers}

class ScriptCacheTest extends FreeSpec with Matchers with WithDB with TransactionGen {

  val CACHE_SIZE = 1
  val AMOUNT     = 10000000000L
  val FEE        = 5000000

  def mkScripts(num: Int): List[Script] = {
    (0 until num).map { ind =>
      val (script, _) = ScriptCompiler(
        s"""
           |let ind = $ind
           |true
          """.stripMargin,
        isAssetScript = false
      ).explicitGet()

      script
    }.toList
  }

  def blockGen(scripts: List[Script], t: Time): Gen[(Seq[KeyPair], Seq[Block])] = {
    val ts = t.correctedTime()
    Gen
      .listOfN(scripts.length, accountGen)
      .map { accounts =>
        for {
          account <- accounts
          i = accounts.indexOf(account)
        } yield (account, GenesisTransaction.create(account.toAddress, AMOUNT, ts + i).explicitGet())
      }
      .map { ag =>
        val (accounts, genesisTxs) = ag.unzip

        val setScriptTxs =
          (accounts zip scripts)
            .map {
              case (account, script) =>
                SetScriptTransaction
                  .selfSigned(account, Some(script), FEE, ts + accounts.length + accounts.indexOf(account) + 1)
                  .explicitGet()
            }

        val genesisBlock = TestBlock.create(genesisTxs)

        val nextBlock =
          TestBlock
            .create(
              time = setScriptTxs.last.timestamp + 1,
              ref = genesisBlock.uniqueId,
              txs = setScriptTxs
            )

        (accounts, genesisBlock +: nextBlock +: Nil)
      }
  }

  "ScriptCache" - {
    "return correct script after overflow" in {
      val scripts = mkScripts(CACHE_SIZE * 10)

      withBlockchain(blockGen(scripts, _)) {
        case (accounts, bc) =>
          val allScriptCorrect = (accounts zip scripts)
            .map {
              case (account, script) =>
                val address = account.toAddress

                val scriptFromCache =
                  bc.accountScript(address)
                    .toRight(s"No script for acc: $account")
                    .explicitGet()

                scriptFromCache == script && bc.hasScript(address)
            }
            .forall(identity)

          allScriptCorrect shouldBe true
      }
    }

    "Return correct script after rollback" in {
      val scripts @ List(script) = mkScripts(1)

      withBlockchain(blockGen(scripts, _)) {
        case (List(account), bcu) =>
          bcu.accountScript(account.toAddress) shouldEqual Some(script)

          val lastBlock = bcu.lastBlock.get

          val newScriptTx = SetScriptTransaction
            .selfSigned(account, None, FEE, lastBlock.timestamp + 1)
            .explicitGet()

          val blockWithEmptyScriptTx = TestBlock
            .create(
              time = lastBlock.timestamp + 2,
              ref = lastBlock.uniqueId,
              txs = Seq(newScriptTx)
            )

          bcu
            .processBlock(blockWithEmptyScriptTx)
            .explicitGet()

          bcu.accountScript(account.toAddress) shouldEqual None
          bcu.removeAfter(lastBlock.uniqueId)
          bcu.accountScript(account.toAddress) shouldEqual Some(script)
      }
    }

  }

  def withBlockchain(gen: Time => Gen[(Seq[KeyPair], Seq[Block])])(f: (Seq[KeyPair], BlockchainUpdater with NG) => Unit): Unit = {
    val settings0     = PlixSettings.fromRootConfig(loadConfig(ConfigFactory.load()))
    val settings      = settings0.copy(featuresSettings = settings0.featuresSettings.copy(autoShutdownOnUnsupportedFeature = false))
    val defaultWriter = new LevelDBWriter(db, ignoreSpendableBalanceChanged, TestFunctionalitySettings.Stub, settings0.dbSettings.copy(maxCacheSize = CACHE_SIZE))
    val bcu           = new BlockchainUpdaterImpl(defaultWriter, ignoreSpendableBalanceChanged, settings, ntpTime)
    try {
      val (accounts, blocks) = gen(ntpTime).sample.get

      blocks.foreach { block =>
        bcu.processBlock(block).explicitGet()
      }

      f(accounts, bcu)
      bcu.shutdown()
    } finally {
      bcu.shutdown()
      db.close()
    }
  }
}
