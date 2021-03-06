package com.plixplatform.it.sync.smartcontract

import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.sync.{setScriptFee, smartFee, smartMinFee}
import com.plixplatform.it.transactions.BaseTransactionSuite
import com.plixplatform.transaction.Asset
import com.plixplatform.transaction.Asset.{IssuedAsset, Plix}
import com.plixplatform.transaction.smart.InvokeScriptTransaction.Payment
import com.plixplatform.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class InvokeScriptPayAndTransferSameAssetSuite extends BaseTransactionSuite with CancelAfterFailure {

  private val dApp     = pkByAddress(firstAddress).address
  private val caller   = pkByAddress(secondAddress).address
  private val receiver = pkByAddress(thirdAddress).address

  var dAppInitBalance: Long     = 0
  var callerInitBalance: Long   = 0
  var receiverInitBalance: Long = 0
  val assetQuantity: Long       = 15
  var assetId: String           = ""
  var smartAssetId: String      = ""
  var rejAssetId: String        = ""

  test("_issue and transfer asset") {
    assetId = sender.issue(caller, "Asset", "a", assetQuantity, 0).id

    val script = Some(ScriptCompiler.compile("true").explicitGet()._1.bytes.value.base64)
    smartAssetId = sender.issue(caller, "Smart", "s", assetQuantity, 0, script = script).id

    val scriptText  = "match tx {case t:TransferTransaction => false case _ => true}"
    val smartScript = Some(ScriptCompiler.compile(scriptText).explicitGet()._1.bytes.value.base64)
    rejAssetId = sender.issue(caller, "Reject", "r", assetQuantity, 0, script = smartScript, waitForTx = true).id
  }

  test("_set script to dApp account and transfer out all plix") {
    val dAppBalance = sender.accountBalances(dApp)._1
    sender.transfer(dApp, caller, dAppBalance - smartMinFee - setScriptFee, smartMinFee, waitForTx = true).id

    val dAppScript        = ScriptCompiler.compile(s"""
          |{-# STDLIB_VERSION 3 #-}
          |{-# CONTENT_TYPE DAPP #-}
          |
          |let receiver = Address(base58'$receiver')
          |
          |@Callable(i)
          |func resendPayment() = {
          |  if (isDefined(i.payment)) then
          |    let pay = extract(i.payment)
          |    TransferSet([ScriptTransfer(receiver, 1, pay.assetId)])
          |  else throw("need payment in PLIX or any Asset")
          |}
        """.stripMargin).explicitGet()._1
    sender.setScript(dApp, Some(dAppScript.bytes().base64), waitForTx = true).id

  }

  test("dApp can transfer payed asset if its own balance is 0") {
    dAppInitBalance = sender.accountBalances(dApp)._1
    callerInitBalance = sender.accountBalances(caller)._1
    receiverInitBalance = sender.accountBalances(receiver)._1

    val paymentAmount = 10

    invoke("resendPayment", paymentAmount, issued(assetId))

    sender.accountBalances(dApp)._1 shouldBe dAppInitBalance
    sender.accountBalances(caller)._1 shouldBe callerInitBalance - smartMinFee
    sender.accountBalances(receiver)._1 shouldBe receiverInitBalance

    sender.assetBalance(dApp, assetId).balance shouldBe paymentAmount - 1
    sender.assetBalance(caller, assetId).balance shouldBe assetQuantity - paymentAmount
    sender.assetBalance(receiver, assetId).balance shouldBe 1
  }

  test("dApp can transfer payed smart asset if its own balance is 0") {
    dAppInitBalance = sender.accountBalances(dApp)._1
    callerInitBalance = sender.accountBalances(caller)._1
    receiverInitBalance = sender.accountBalances(receiver)._1

    val paymentAmount = 10
    val fee           = smartMinFee + smartFee * 2

    invoke("resendPayment", paymentAmount, issued(smartAssetId), fee)

    sender.accountBalances(dApp)._1 shouldBe dAppInitBalance
    sender.accountBalances(caller)._1 shouldBe callerInitBalance - fee
    sender.accountBalances(receiver)._1 shouldBe receiverInitBalance

    sender.assetBalance(dApp, smartAssetId).balance shouldBe paymentAmount - 1
    sender.assetBalance(caller, smartAssetId).balance shouldBe assetQuantity - paymentAmount
    sender.assetBalance(receiver, smartAssetId).balance shouldBe 1
  }

  test("dApp can't transfer payed smart asset if it rejects transfers and its own balance is 0") {
    dAppInitBalance = sender.accountBalances(dApp)._1
    callerInitBalance = sender.accountBalances(caller)._1
    receiverInitBalance = sender.accountBalances(receiver)._1

    val paymentAmount = 10
    val fee           = smartMinFee + smartFee * 2

    assertBadRequestAndMessage(
      invoke("resendPayment", paymentAmount, issued(rejAssetId), fee),
      "token-script"
    )
  }

  test("dApp can transfer payed Plix if its own balance is 0") {
    dAppInitBalance = sender.accountBalances(dApp)._1
    callerInitBalance = sender.accountBalances(caller)._1
    receiverInitBalance = sender.accountBalances(receiver)._1

    dAppInitBalance shouldBe 0

    val paymentAmount    = 10
    invoke("resendPayment", paymentAmount)

    sender.accountBalances(dApp)._1 shouldBe dAppInitBalance + paymentAmount - 1
    sender.accountBalances(caller)._1 shouldBe callerInitBalance - paymentAmount - smartMinFee
    sender.accountBalances(receiver)._1 shouldBe receiverInitBalance + 1
  }

  def issued(assetId: String): Asset = IssuedAsset(ByteStr.decodeBase58(assetId).get)

  def invoke(func: String, amount: Long, asset: Asset = Plix, fee: Long = 500000): String = {
    sender
      .invokeScript(
        caller,
        dApp,
        Some(func),
        payment = Seq(Payment(amount, asset)),
        fee = fee,
        waitForTx = true
      )
      .id
  }

}
