package com.plixplatform.it.sync.smartcontract

import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.sync.{minFee, setScriptFee}
import com.plixplatform.it.transactions.BaseTransactionSuite
import com.plixplatform.it.util._
import com.plixplatform.lang.v1.compiler.Terms.CONST_LONG
import com.plixplatform.state._
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.smart.InvokeScriptTransaction
import com.plixplatform.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class HodlContractTransactionSuite extends BaseTransactionSuite with CancelAfterFailure {

  private val contract = pkByAddress(firstAddress)
  private val caller   = pkByAddress(secondAddress)

  test("setup contract account with plix") {
    sender
      .transfer(
        sender.address,
        recipient = contract.address,
        assetId = None,
        amount = 5.plix,
        fee = minFee,
        waitForTx = true
      )
      .id
  }

  test("setup caller account with plix") {
    sender
      .transfer(
        sender.address,
        recipient = caller.address,
        assetId = None,
        amount = 10.plix,
        fee = minFee,
        waitForTx = true
      )
      .id
  }

  test("set contract to contract account") {
    val scriptText =
      """
        |{-# STDLIB_VERSION 3 #-}
        |{-# CONTENT_TYPE DAPP #-}
        |
        |	@Callable(i)
        |	func deposit() = {
        |   let pmt = extract(i.payment)
        |   if (isDefined(pmt.assetId)) then throw("can hodl plix only at the moment")
        |   else {
        |	  	let currentKey = toBase58String(i.caller.bytes)
        |	  	let currentAmount = match getInteger(this, currentKey) {
        |	  		case a:Int => a
        |	  		case _ => 0
        |	  	}
        |	  	let newAmount = currentAmount + pmt.amount
        |	  	WriteSet([DataEntry(currentKey, newAmount)])
        |
        |   }
        |	}
        |
        | @Callable(i)
        | func withdraw(amount: Int) = {
        |	  	let currentKey = toBase58String(i.caller.bytes)
        |	  	let currentAmount = match getInteger(this, currentKey) {
        |	  		case a:Int => a
        |	  		case _ => 0
        |	  	}
        |		let newAmount = currentAmount - amount
        |	 if (amount < 0)
        |			then throw("Can't withdraw negative amount")
        |  else if (newAmount < 0)
        |			then throw("Not enough balance")
        |			else  ScriptResult(
        |					WriteSet([DataEntry(currentKey, newAmount)]),
        |					TransferSet([ScriptTransfer(i.caller, amount, unit)])
        |				)
        |	}
        """.stripMargin

    val script      = ScriptCompiler.compile(scriptText).explicitGet()._1.bytes().base64
    val setScriptId = sender.setScript(contract.address, Some(script), setScriptFee, waitForTx = true).id

    val acc0ScriptInfo = sender.addressScriptInfo(contract.address)

    acc0ScriptInfo.script.isEmpty shouldBe false
    acc0ScriptInfo.scriptText.isEmpty shouldBe false
    acc0ScriptInfo.script.get.startsWith("base64:") shouldBe true

    sender.transactionInfo(setScriptId).script.get.startsWith("base64:") shouldBe true
  }

  test("caller deposits plix") {
    val balanceBefore = sender.accountBalances(contract.address)._1
    val invokeScriptId = sender
      .invokeScript(
        caller.address,
        dappAddress = contract.address,
        func = Some("deposit"),
        args = List.empty,
        payment = Seq(InvokeScriptTransaction.Payment(1.5.plix, Plix)),
        fee = 1.plix,
        waitForTx = true
      )
      .id

    sender.waitForTransaction(invokeScriptId)

    sender.getDataByKey(contract.address, caller.address) shouldBe IntegerDataEntry(caller.address, 1.5.plix)
    val balanceAfter = sender.accountBalances(contract.address)._1

    (balanceAfter - balanceBefore) shouldBe 1.5.plix
  }

  test("caller can't withdraw more than owns") {
    assertBadRequestAndMessage(
      sender.invokeScript(
        caller.address,
        contract.address,
        func = Some("withdraw"),
        args = List(CONST_LONG(1.51.plix)),
        payment = Seq(),
        fee = 1.plix
      ),
      "Not enough balance"
    )
  }

  test("caller can withdraw less than he owns") {
    val balanceBefore = sender.accountBalances(contract.address)._1
    val invokeScriptId = sender
      .invokeScript(
        caller.address,
        dappAddress = contract.address,
        func = Some("withdraw"),
        args = List(CONST_LONG(1.49.plix)),
        payment = Seq(),
        fee = 1.plix,
        waitForTx = true
      )
      .id

    val balanceAfter = sender.accountBalances(contract.address)._1

    sender.getDataByKey(contract.address, caller.address) shouldBe IntegerDataEntry(caller.address, 0.01.plix)
    (balanceAfter - balanceBefore) shouldBe -1.49.plix

    val stateChangesInfo = sender.debugStateChanges(invokeScriptId).stateChanges

    val stateChangesData = stateChangesInfo.get.data.head
    stateChangesInfo.get.data.length shouldBe 1
    stateChangesData.`type` shouldBe "integer"
    stateChangesData.value shouldBe 0.01.plix

    val stateChangesTransfers = stateChangesInfo.get.transfers.head
    stateChangesInfo.get.transfers.length shouldBe 1
    stateChangesTransfers.address shouldBe caller.address
    stateChangesTransfers.amount shouldBe 1.49.plix
    stateChangesTransfers.asset shouldBe None
  }

}
