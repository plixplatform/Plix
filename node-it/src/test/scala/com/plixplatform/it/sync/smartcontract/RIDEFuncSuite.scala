package com.plixplatform.it.sync.smartcontract

import com.typesafe.config.Config
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.it.NodeConfigs
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.sync._
import com.plixplatform.it.transactions.BaseTransactionSuite
import com.plixplatform.it.util._
import com.plixplatform.transaction.Asset.{IssuedAsset, Plix}
import com.plixplatform.transaction.smart.SetScriptTransaction
import com.plixplatform.transaction.smart.script.ScriptCompiler
import com.plixplatform.transaction.transfer.TransferTransactionV2
import org.scalatest.CancelAfterFailure

class RIDEFuncSuite extends BaseTransactionSuite with CancelAfterFailure {
  override protected def nodeConfigs: Seq[Config] =
    NodeConfigs.newBuilder
      .overrideBase(_.quorum(0))
      .withDefault(entitiesNumber = 1)
      .buildNonConflicting()

  private val acc0 = pkByAddress(firstAddress)

  test("assetBalance() verification") {
    val asset = sender
      .issue(acc0.address, "SomeCoin", "SomeDescription", someAssetAmount, 0, reissuable = false, issueFee, 2, waitForTx = true)
      .id

    val newAddress   = sender.createAddress()
    val pkNewAddress = pkByAddress(newAddress)

    sender.transfer(acc0.address, newAddress, 10.plix, minFee, waitForTx = true)

    val scriptSrc =
      s"""
         |match tx {
         |  case tx : SetScriptTransaction => true
         |  case other => assetBalance(tx.sender, base58'$asset') > 0
         |}
      """.stripMargin

    val compiled = ScriptCompiler(scriptSrc, isAssetScript = false).explicitGet()._1

    val tx =
      sender.signedBroadcast(
        SetScriptTransaction.selfSigned(pkNewAddress, Some(compiled), setScriptFee, System.currentTimeMillis()).explicitGet().json())
    nodes.waitForHeightAriseAndTxPresent(tx.id)

    assertBadRequestAndResponse(
      sender.signedBroadcast(
        TransferTransactionV2
          .selfSigned(Plix, pkNewAddress, pkNewAddress, 1.plix, System.currentTimeMillis(), Plix, smartMinFee, Array())
          .explicitGet()
          .json()),
      "Transaction is not allowed by account-script"
    )

    sender.signedBroadcast(
      TransferTransactionV2
        .selfSigned(IssuedAsset(ByteStr.decodeBase58(asset).get),
                    acc0,
                    pkNewAddress,
                    100000000,
                    System.currentTimeMillis(),
                    Plix,
                    smartMinFee,
                    Array())
        .explicitGet()
        .json(),
      waitForTx = true
    )

    val transfer = sender.signedBroadcast(
      TransferTransactionV2
        .selfSigned(Plix, pkNewAddress, pkNewAddress, 1.plix, System.currentTimeMillis(), Plix, smartMinFee, Array())
        .explicitGet()
        .json())
    nodes.waitForHeightAriseAndTxPresent(transfer.id)

    val udpatedScript =
      s"""
         |match tx {
         |  case tx : SetScriptTransaction => true
         |  case other => assetBalance(tx.sender, base58'$asset') >= 900000000 && plixBalance(tx.sender) >500000000
         |}
      """.stripMargin

    val updated = ScriptCompiler(udpatedScript, isAssetScript = false).explicitGet()._1

    val updTx =
      sender.signedBroadcast(
        SetScriptTransaction.selfSigned(pkNewAddress, Some(updated), setScriptFee + smartFee, System.currentTimeMillis()).explicitGet().json())
    nodes.waitForHeightAriseAndTxPresent(updTx.id)

    assertBadRequestAndResponse(
      sender.signedBroadcast(
        TransferTransactionV2
          .selfSigned(Plix, pkNewAddress, pkNewAddress, 1.plix, System.currentTimeMillis(), Plix, smartMinFee, Array())
          .explicitGet()
          .json()),
      "Transaction is not allowed by account-script"
    )

    sender.signedBroadcast(
      TransferTransactionV2
        .selfSigned(IssuedAsset(ByteStr.decodeBase58(asset).get),
                    acc0,
                    pkNewAddress,
                    800000000,
                    System.currentTimeMillis(),
                    Plix,
                    smartMinFee,
                    Array())
        .explicitGet()
        .json(),
      waitForTx = true
    )

    val transferAfterUpd = sender.signedBroadcast(
      TransferTransactionV2
        .selfSigned(Plix, pkNewAddress, pkNewAddress, 1.plix, System.currentTimeMillis(), Plix, smartMinFee, Array())
        .explicitGet()
        .json())
    nodes.waitForHeightAriseAndTxPresent(transferAfterUpd.id)
  }

  test("split around empty string") {
    val scriptText =
      s"""
         |  {-# STDLIB_VERSION 3       #-}
         |  {-# CONTENT_TYPE   DAPP    #-}
         |  {-# SCRIPT_TYPE    ACCOUNT #-}
         |
         |  @Verifier(tx)
         |  func verify() = {
         |    let strs = split("some", "")
         |    strs.size() == 4  &&
         |    strs[0] == "s"    &&
         |    strs[1] == "o"    &&
         |    strs[2] == "m"    &&
         |    strs[3] == "e"
         |  }
      """.stripMargin

    val compiledScript = ScriptCompiler.compile(scriptText).explicitGet()._1

    val newAddress   = sender.createAddress()
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

  test("lastBlock and blockInfoByHeight(last) must return liquid block") {
    val script = ScriptCompiler.compile(
      s"""
         |  {-# STDLIB_VERSION 3       #-}
         |  {-# CONTENT_TYPE   DAPP    #-}
         |  {-# SCRIPT_TYPE    ACCOUNT #-}
         |
         |  @Verifier(tx)
         |  func verify() = {
         |    let block = extract(blockInfoByHeight(height))
         |
         |    let checkTs = lastBlock.timestamp == block.timestamp
         |    let checkHeight = block.height == height
         |    let checkHeightLast = lastBlock.height == height
         |    checkTs && checkHeight && checkHeightLast
         |  }
      """.stripMargin).explicitGet()._1

    val newAddress = sender.createAddress()
    sender.transfer(acc0.address, newAddress, 10.plix, minFee, waitForTx = true)

    val setScript = sender.setScript(newAddress, Some(script.bytes().base64), setScriptFee)
    nodes.waitForHeightAriseAndTxPresent(setScript.id)

    val transfer = sender.transfer(newAddress, newAddress, 1.plix, minFee + (2 * smartFee))
    nodes.waitForHeightAriseAndTxPresent(transfer.id)
  }
}
