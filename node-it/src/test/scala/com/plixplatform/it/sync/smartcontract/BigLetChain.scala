package com.plixlatform.it.sync.smartcontract

import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.transaction.smart.SetScriptTransaction
import com.plixlatform.transaction.smart.script.ScriptCompiler
import com.plixlatform.transaction.transfer.TransferTransactionV2
import com.plixlatform.it.api.SyncHttpApi._
import com.plixlatform.it.sync._
import com.plixlatform.it.transactions.BaseTransactionSuite
import com.plixlatform.it.util._
import org.scalatest.CancelAfterFailure
import com.plixlatform.common.utils.EitherExt2

class BigLetChain extends BaseTransactionSuite with CancelAfterFailure {
  test("big let assignment chain") {
    val count = 550
    val scriptText =
      s"""
         | {-# STDLIB_VERSION 3    #-}
         | {-# CONTENT_TYPE   DAPP #-}
         |
         | @Verifier(tx)
         | func verify() = {
         |   let a0 = 1
         |   ${1 to count map (i => s"let a$i = a${i - 1}") mkString "\n"}
         |   a$count == a$count
         | }
       """.stripMargin

    val compiledScript = ScriptCompiler.compile(scriptText).explicitGet()._1

    val newAddress   = sender.createAddress()
    val acc0         = pkByAddress(firstAddress)
    val pkNewAddress = pkByAddress(newAddress)

    sender.transfer(acc0.address, newAddress, 10.plix, minFee, waitForTx = true)

    val scriptSet = SetScriptTransaction.selfSigned(
      pkNewAddress,
      Some(compiledScript),
      setScriptFee,
      System.currentTimeMillis()
    )
    val scriptSetBroadcast = sender.signedBroadcast(scriptSet.explicitGet().json.value)
    nodes.waitForHeightAriseAndTxPresent(scriptSetBroadcast.id)

    val transfer = TransferTransactionV2.selfSigned(
      Plix,
      pkNewAddress,
      pkNewAddress,
      1.plix,
      System.currentTimeMillis(),
      Plix,
      smartMinFee,
      Array()
    )
    val transferBroadcast = sender.signedBroadcast(transfer.explicitGet().json.value)
    nodes.waitForHeightAriseAndTxPresent(transferBroadcast.id)
  }
}
