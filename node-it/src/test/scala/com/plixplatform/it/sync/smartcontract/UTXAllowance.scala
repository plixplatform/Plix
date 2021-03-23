package com.plixplatform.it.sync.smartcontract

import com.typesafe.config.{Config, ConfigFactory}
import com.plixplatform.account.KeyPair
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.sync._
import com.plixplatform.it.transactions.NodesFromDocker
import com.plixplatform.it.util._
import com.plixplatform.it.{ReportingTestName, WaitForHeight2}
import com.plixplatform.transaction.smart.script.ScriptCompiler
import org.scalatest.{CancelAfterFailure, FreeSpec, Matchers}

class UTXAllowance extends FreeSpec with Matchers with WaitForHeight2 with CancelAfterFailure with ReportingTestName with NodesFromDocker {
  import UTXAllowance._

  override protected def nodeConfigs: Seq[Config] = Configs

  private def nodeA = nodes.head
  private def nodeB = nodes.last

  "create two nodes with scripted accounts and check UTX" in {
    val accounts = List(nodeA, nodeB).map(i => {

      val nodeAddress = i.createAddress()
      val acc         = KeyPair.fromSeed(i.seed(nodeAddress)).right.get

      i.transfer(i.address, nodeAddress, 10.plix, 0.005.plix, None, waitForTx = true)

      val scriptText = s"""true""".stripMargin
      val script               = ScriptCompiler(scriptText, isAssetScript = false).explicitGet()._1.bytes().base64
      i.setScript(acc.address, Some(script), setScriptFee, waitForTx = true)

      acc
    })

    assertBadRequestAndMessage(
      nodeA
        .transfer(
          accounts.head.address,
          recipient = accounts.head.address,
          assetId = None,
          amount = 1.plix,
          fee = minFee + 0.004.plix,
          version = 2
        ),
      "transactions from scripted accounts are denied from UTX pool"
    )

    val txBId =
      nodeB
        .transfer(
          accounts(1).address,
          recipient = accounts(1).address,
          assetId = None,
          amount = 1.01.plix,
          fee = minFee + 0.004.plix,
          version = 2
        )
        .id

    nodes.waitForHeightArise()
    nodeA.findTransactionInfo(txBId) shouldBe None
  }

}

object UTXAllowance {
  import com.plixplatform.it.NodeConfigs._
  private val FirstNode = ConfigFactory.parseString(s"""
                                                         |plix {
                                                         |  utx.allow-transactions-from-smart-accounts = false
                                                         |  miner {
                                                         |      quorum = 0
                                                         |      enable = yes
                                                         |  }
                                                         |}""".stripMargin)

  private val SecondNode = ConfigFactory.parseString(s"""
                                                          |plix {
                                                          |  utx.allow-transactions-from-smart-accounts = true
                                                          |  miner {
                                                          |      enable = no
                                                          |  }
                                                          |}""".stripMargin)

  val Configs: Seq[Config] = Seq(
    FirstNode.withFallback(Default.head),
    SecondNode.withFallback(Default(1))
  )

}
