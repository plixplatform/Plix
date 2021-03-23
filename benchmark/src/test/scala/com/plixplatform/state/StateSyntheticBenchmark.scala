package com.plixplatform.state

import java.util.concurrent.TimeUnit

import com.plixplatform.account.KeyPair
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.lang.directives.values._
import com.plixplatform.lang.script.v1.ExprScript
import com.plixplatform.lang.utils._
import com.plixplatform.lang.v1.compiler.ExpressionCompiler
import com.plixplatform.lang.v1.parser.Parser
import com.plixplatform.settings.FunctionalitySettings
import com.plixplatform.state.StateSyntheticBenchmark._
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.Transaction
import com.plixplatform.transaction.smart.SetScriptTransaction
import com.plixplatform.transaction.transfer._
import org.openjdk.jmh.annotations._
import org.scalacheck.Gen

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
@Threads(1)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
class StateSyntheticBenchmark {

  @Benchmark
  def appendBlock_test(db: St): Unit = db.genAndApplyNextBlock()

  @Benchmark
  def appendBlock_smart_test(db: SmartSt): Unit = db.genAndApplyNextBlock()

}

object StateSyntheticBenchmark {

  @State(Scope.Benchmark)
  class St extends BaseState {
    protected override def txGenP(sender: KeyPair, ts: Long): Gen[Transaction] =
      for {
        amount    <- Gen.choose(1, plix(1))
        recipient <- accountGen
      } yield TransferTransactionV1.selfSigned(Plix, sender, recipient, amount, ts, Plix, 100000, Array.emptyByteArray).explicitGet()
  }

  @State(Scope.Benchmark)
  class SmartSt extends BaseState {

    override protected def updateFunctionalitySettings(base: FunctionalitySettings): FunctionalitySettings = {
      base.copy(preActivatedFeatures = Map(4.toShort -> 0))
    }

    protected override def txGenP(sender: KeyPair, ts: Long): Gen[Transaction] =
      for {
        recipient: KeyPair <- accountGen
        amount                    <- Gen.choose(1, plix(1))
      } yield
        TransferTransactionV2
          .selfSigned(
            Plix,
            sender,
            recipient.toAddress,
            amount,
            ts,
            Plix,
            1000000,
            Array.emptyByteArray
          )
          .explicitGet()

    @Setup
    override def init(): Unit = {
      super.init()

      val textScript    = "sigVerify(tx.bodyBytes,tx.proofs[0],tx.senderPublicKey)"
      val untypedScript = Parser.parseExpr(textScript).get.value
      val typedScript   = ExpressionCompiler(compilerContext(V1, Expression, isAssetScript = false), untypedScript).explicitGet()._1

      val setScriptBlock = nextBlock(
        Seq(
          SetScriptTransaction
            .selfSigned(
              richAccount,
              Some(ExprScript(typedScript).explicitGet()),
              1000000,
              System.currentTimeMillis()
            )
            .explicitGet()
        )
      )

      applyBlock(setScriptBlock)
    }
  }

}
