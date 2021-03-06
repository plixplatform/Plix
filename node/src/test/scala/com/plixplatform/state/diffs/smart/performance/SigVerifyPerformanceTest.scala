package com.plixplatform.state.diffs.smart.performance

import com.plixplatform.account.{KeyPair, PublicKey}
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.lagonaki.mocks.TestBlock
import com.plixplatform.lang.directives.values._
import com.plixplatform.lang.v1.compiler.ExpressionCompiler
import com.plixplatform.lang.v1.compiler.Terms._
import com.plixplatform.lang.v1.parser.Parser
import com.plixplatform.metrics.Instrumented
import com.plixplatform.state.diffs._
import com.plixplatform.state.diffs.smart._
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.GenesisTransaction
import com.plixplatform.lang.script.v1.ExprScript
import com.plixplatform.transaction.transfer._
import com.plixplatform.lang.utils._
import com.plixplatform.{NoShrink, TransactionGen, WithDB}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class SigVerifyPerformanceTest extends PropSpec with PropertyChecks with Matchers with TransactionGen with NoShrink with WithDB {

  private val AmtOfTxs = 10000

  private def simpleSendGen(from: KeyPair, to: PublicKey, ts: Long): Gen[TransferTransactionV1] =
    for {
      amt <- smallFeeGen
      fee <- smallFeeGen
    } yield TransferTransactionV1.selfSigned(Plix, from, to.toAddress, amt, ts, Plix, fee, Array.emptyByteArray).explicitGet()

  private def scriptedSendGen(from: KeyPair, to: PublicKey, ts: Long): Gen[TransferTransactionV2] =
    for {
      amt <- smallFeeGen
      fee <- smallFeeGen
    } yield TransferTransactionV2.selfSigned(Plix, from, to.toAddress, amt, ts, Plix, fee, Array.emptyByteArray).explicitGet()

  private def differentTransfers(typed: EXPR) =
    for {
      master    <- accountGen
      recipient <- accountGen
      ts        <- positiveIntGen
      amt       <- smallFeeGen
      fee       <- smallFeeGen
      genesis = GenesisTransaction.create(master, ENOUGH_AMT, ts).explicitGet()
      setScript <- selfSignedSetScriptTransactionGenP(master, ExprScript(typed).explicitGet())
      transfer       = simpleSendGen(master, recipient, ts)
      scriptTransfer = scriptedSendGen(master, recipient, ts)
      transfers       <- Gen.listOfN(AmtOfTxs, transfer)
      scriptTransfers <- Gen.listOfN(AmtOfTxs, scriptTransfer)
    } yield (genesis, setScript, transfers, scriptTransfers)

  ignore("parallel native signature verification vs sequential scripted signature verification") {
    val textScript    = "sigVerify(tx.bodyBytes,tx.proofs[0],tx.senderPk)"
    val untypedScript = Parser.parseExpr(textScript).get.value
    val typedScript   = ExpressionCompiler(compilerContext(V1, Expression, isAssetScript = false), untypedScript).explicitGet()._1

    forAll(differentTransfers(typedScript)) {
      case (gen, setScript, transfers, scriptTransfers) =>
        def simpleCheck(): Unit = assertDiffAndState(Seq(TestBlock.create(Seq(gen))), TestBlock.create(transfers), smartEnabledFS) { case _ => }
        def scriptedCheck(): Unit =
          assertDiffAndState(Seq(TestBlock.create(Seq(gen, setScript))), TestBlock.create(scriptTransfers), smartEnabledFS) {
            case _ =>
          }

        val simeplCheckTime   = Instrumented.withTimeMillis(simpleCheck())._2
        val scriptedCheckTime = Instrumented.withTimeMillis(scriptedCheck())._2
        println(s"[parallel] simple check time: $simeplCheckTime ms,\t [seqential] scripted check time: $scriptedCheckTime ms")
    }

  }
}
