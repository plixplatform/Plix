package com.plixplatform.it.sync.smartcontract

import com.plixplatform.account.AddressScheme
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.crypto
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.sync.{minFee, setScriptFee, transferAmount}
import com.plixplatform.it.transactions.BaseTransactionSuite
import com.plixplatform.it.util._
import com.plixplatform.transaction.Proofs
import com.plixplatform.transaction.lease.{LeaseCancelTransactionV2, LeaseTransactionV2}
import com.plixplatform.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class LeaseSmartContractsTestSuite extends BaseTransactionSuite with CancelAfterFailure {
  private val acc0 = pkByAddress(firstAddress)
  private val acc1 = pkByAddress(secondAddress)
  private val acc2 = pkByAddress(thirdAddress)

  test("set contract, make leasing and cancel leasing") {
    val (balance1, eff1) = miner.accountBalances(acc0.address)
    val (balance2, eff2) = miner.accountBalances(thirdAddress)

    sender.transfer(sender.address, acc0.address, 10 * transferAmount, minFee, waitForTx = true).id

    miner.assertBalances(firstAddress, balance1 + 10 * transferAmount, eff1 + 10 * transferAmount)

    val scriptText = s"""
        let pkA = base58'${ByteStr(acc0.publicKey)}'
        let pkB = base58'${ByteStr(acc1.publicKey)}'
        let pkC = base58'${ByteStr(acc2.publicKey)}'

        match tx {
          case ltx: LeaseTransaction => sigVerify(ltx.bodyBytes,ltx.proofs[0],pkA) && sigVerify(ltx.bodyBytes,ltx.proofs[2],pkC)
          case lctx : LeaseCancelTransaction => sigVerify(lctx.bodyBytes,lctx.proofs[1],pkA) && sigVerify(lctx.bodyBytes,lctx.proofs[2],pkB)
          case other => false
        }
        """.stripMargin

    val script = ScriptCompiler(scriptText, isAssetScript = false).explicitGet()._1.bytes().base64
    sender.setScript(acc0.address, Some(script), setScriptFee, waitForTx = true).id

    val unsignedLeasing =
      LeaseTransactionV2
        .create(
          acc0,
          transferAmount,
          minFee + 0.2.plix,
          System.currentTimeMillis(),
          acc2,
          Proofs.empty
        )
        .explicitGet()

    val sigLeasingA = ByteStr(crypto.sign(acc0, unsignedLeasing.bodyBytes()))
    val sigLeasingC = ByteStr(crypto.sign(acc2, unsignedLeasing.bodyBytes()))

    val signedLeasing =
      unsignedLeasing.copy(proofs = Proofs(Seq(sigLeasingA, ByteStr.empty, sigLeasingC)))

    val leasingId =
      sender.signedBroadcast(signedLeasing.json(), waitForTx = true).id

    miner.assertBalances(firstAddress,
                         balance1 + 10 * transferAmount - (minFee + setScriptFee + 0.2.plix),
                         eff1 + 9 * transferAmount - (minFee + setScriptFee + 0.2.plix))
    miner.assertBalances(thirdAddress, balance2, eff2 + transferAmount)

    val unsignedCancelLeasing =
      LeaseCancelTransactionV2
        .create(
          chainId = AddressScheme.current.chainId,
          sender = acc0,
          leaseId = ByteStr.decodeBase58(leasingId).get,
          fee = minFee + 0.2.plix,
          timestamp = System.currentTimeMillis(),
          proofs = Proofs.empty
        )
        .explicitGet()

    val sigLeasingCancelA = ByteStr(crypto.sign(acc0, unsignedCancelLeasing.bodyBytes()))
    val sigLeasingCancelB = ByteStr(crypto.sign(acc1, unsignedCancelLeasing.bodyBytes()))

    val signedLeasingCancel =
      unsignedCancelLeasing.copy(proofs = Proofs(Seq(ByteStr.empty, sigLeasingCancelA, sigLeasingCancelB)))

    sender.signedBroadcast(signedLeasingCancel.json(), waitForTx = true).id

    miner.assertBalances(firstAddress,
                         balance1 + 10 * transferAmount - (2 * minFee + setScriptFee + 2 * 0.2.plix),
                         eff1 + 10 * transferAmount - (2 * minFee + setScriptFee + 2 * 0.2.plix))
    miner.assertBalances(thirdAddress, balance2, eff2)

  }
}
