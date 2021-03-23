package com.plixlatform.it.sync.transactions

import com.plixlatform.it.api.SyncHttpApi._
import com.plixlatform.it.transactions.BaseTransactionSuite
import com.plixlatform.it.util._
import com.plixlatform.it.sync._

class ReissueTransactionV1Suite extends BaseTransactionSuite {

  test("asset reissue changes issuer's asset balance; issuer's plix balance is decreased by fee") {

    val (balance, effectiveBalance) = miner.accountBalances(firstAddress)

    val issuedAssetId = sender.issue(firstAddress, "name2", "description2", someAssetAmount, decimals = 2, reissuable = true, fee = issueFee).id
    nodes.waitForHeightAriseAndTxPresent(issuedAssetId)
    miner.assertBalances(firstAddress, balance - issueFee, effectiveBalance - issueFee)
    miner.assertAssetBalance(firstAddress, issuedAssetId, someAssetAmount)

    val reissueTxId = sender.reissue(firstAddress, issuedAssetId, someAssetAmount, reissuable = true, fee = issueFee).id
    nodes.waitForHeightAriseAndTxPresent(reissueTxId)
    miner.assertBalances(firstAddress, balance - 2 * issueFee, effectiveBalance - 2 * issueFee)
    miner.assertAssetBalance(firstAddress, issuedAssetId, 2 * someAssetAmount)
  }

  test("can't reissue not reissuable asset") {
    val (balance, effectiveBalance) = miner.accountBalances(firstAddress)

    val issuedAssetId = sender.issue(firstAddress, "name2", "description2", someAssetAmount, decimals = 2, reissuable = false, issueFee).id
    nodes.waitForHeightAriseAndTxPresent(issuedAssetId)
    miner.assertBalances(firstAddress, balance - issueFee, effectiveBalance - issueFee)
    miner.assertAssetBalance(firstAddress, issuedAssetId, someAssetAmount)

    assertBadRequestAndMessage(sender.reissue(firstAddress, issuedAssetId, someAssetAmount, reissuable = true, fee = issueFee),
                               "Asset is not reissuable")
    nodes.waitForHeightArise()

    miner.assertAssetBalance(firstAddress, issuedAssetId, someAssetAmount)
    miner.assertBalances(firstAddress, balance - issueFee, effectiveBalance - issueFee)
  }

  test("not able to reissue if cannot pay fee - insufficient funds") {

    val (balance, effectiveBalance) = miner.accountBalances(firstAddress)
    val reissueFee                  = effectiveBalance + 1.plix

    val issuedAssetId = sender.issue(firstAddress, "name3", "description3", someAssetAmount, decimals = 2, reissuable = true, issueFee).id

    nodes.waitForHeightAriseAndTxPresent(issuedAssetId)

    assertBadRequestAndMessage(sender.reissue(firstAddress, issuedAssetId, someAssetAmount, reissuable = true, fee = reissueFee),
                               "negative plix balance")
    nodes.waitForHeightArise()

    miner.assertAssetBalance(firstAddress, issuedAssetId, someAssetAmount)
    miner.assertBalances(firstAddress, balance - issueFee, effectiveBalance - issueFee)

  }

}
