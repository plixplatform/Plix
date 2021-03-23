package com.plixplatform.transaction.smart.script

import cats.kernel.Monoid
import com.plixplatform.account.{Address, PublicKey}
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.{Base58, EitherExt2}
import com.plixplatform.lang.directives.values._
import com.plixplatform.lang.directives.{DirectiveDictionary, DirectiveSet}
import com.plixplatform.lang.{Global, utils}
import com.plixplatform.lang.v1.compiler.{ExpressionCompiler, _}
import com.plixplatform.lang.v1.evaluator.ctx.impl.plix.PlixContext
import com.plixplatform.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.plixplatform.lang.v1.parser.Expressions.EXPR
import com.plixplatform.lang.v1.parser.Parser
import com.plixplatform.lang.v1.testing.TypedScriptGen
import com.plixplatform.lang.v1.{CTX, FunctionHeader, ScriptEstimator}
import com.plixplatform.state.diffs.smart.predef.scriptWithAllV1Functions
import com.plixplatform.state.{BinaryDataEntry, BooleanDataEntry, IntegerDataEntry, StringDataEntry}
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.smart.PlixEnvironment
import com.plixplatform.transaction.transfer.TransferTransactionV2
import com.plixplatform.transaction.{DataTransaction, Proofs}
import com.plixplatform.utils.EmptyBlockchain
import monix.eval.Coeval
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import scorex.crypto.encode.Base64

class FunctionComplexityTest extends PropSpec with PropertyChecks with Matchers with TypedScriptGen {

  private def estimate(expr: Terms.EXPR, ctx: CTX, funcCosts: Map[FunctionHeader, Coeval[Long]]): Either[String, Long] = {
    ScriptEstimator(ctx.evaluationContext.letDefs.keySet, funcCosts, expr)
  }

  private val ctxV1 = {
    utils.functionCosts(V1)
    Monoid
      .combineAll(
        Seq(
          PureContext.build(Global, V1),
          CryptoContext.build(Global, V1),
          PlixContext.build(
            DirectiveSet(V1, Account, Expression).explicitGet(),
            new PlixEnvironment('K'.toByte, Coeval(???), Coeval(???), EmptyBlockchain, Coeval(???)),
          )
        ))
  }

  private val ctxV2 = {
    utils.functionCosts(V2)
    Monoid
      .combineAll(
        Seq(
          PureContext.build(Global, V2),
          CryptoContext.build(Global, V2),
          PlixContext.build(
            DirectiveSet(V2, Account, Expression).explicitGet(),
            new PlixEnvironment('K'.toByte, Coeval(???), Coeval(???), EmptyBlockchain, Coeval(???))
          )
        ))
  }

  private val ctxV3 = {
    utils.functionCosts(V3)
    Monoid
      .combineAll(
        Seq(
          PureContext.build(Global, V3),
          CryptoContext.build(Global, V3),
          PlixContext.build(
            DirectiveSet(V3, Account, Expression).explicitGet(),
            new PlixEnvironment('K'.toByte, Coeval(???), Coeval(???), EmptyBlockchain, Coeval(???))
          )
        ))
  }

  private def getAllFuncExpression(version: StdLibVersion): EXPR = {
    val entry1 = IntegerDataEntry("int", 24)
    val entry2 = BooleanDataEntry("bool", true)
    val entry3 = BinaryDataEntry("blob", ByteStr(Base64.decode("YWxpY2U=")))
    val entry4 = StringDataEntry("str", "test")

    val dtx = DataTransaction
      .create(
        PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").right.get,
        List(entry1, entry2, entry3, entry4),
        100000,
        1526911531530L,
        Proofs(Seq(ByteStr.decodeBase58("32mNYSefBTrkVngG5REkmmGAVv69ZvNhpbegmnqDReMTmXNyYqbECPgHgXrX2UwyKGLFS45j7xDFyPXjF8jcfw94").get))
      )
      .right
      .get

    val ttx = TransferTransactionV2
      .create(
        Plix,
        PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").right.get,
        Address.fromString("3JGXFfC7P6oyvv3gXohbLoRzSvQWZeFBNNB").right.get,
        100000000,
        1526641218066L,
        Plix,
        100000000,
        Base58.tryDecodeWithLimit("4t2Xazb2SX").get,
        Proofs(Seq(ByteStr.decodeBase58("4bfDaqBcnK3hT8ywFEFndxtS1DTSYfncUqd4s5Vyaa66PZHawtC73rDswUur6QZu5RpqM7L9NFgBHT1vhCoox4vi").get))
      )
      .right
      .get

    val script = scriptWithAllV1Functions(dtx, ttx)
    val adaptedScript =
      if (version == V3) script.replace("transactionById", "transferTransactionById")
      else script

    Parser.parseExpr(adaptedScript).get.value
  }

  property("func complexity map size is equal stdLib SupportedVersions count") {
    val supportedVersionCount = DirectiveDictionary[StdLibVersion].all.size

    ctxV1.functions.foreach { func =>
      func.costByLibVersion.size shouldBe supportedVersionCount
    }

    ctxV2.functions.foreach { func =>
      func.costByLibVersion.size shouldBe supportedVersionCount
    }

    ctxV3.functions.foreach { func =>
      func.costByLibVersion.size shouldBe supportedVersionCount
    }
  }

  property("estimate script with all functions") {
    val exprV1 = ExpressionCompiler(ctxV1.compilerContext, getAllFuncExpression(V1)).explicitGet()._1
    estimate(exprV1, ctxV1, utils.functionCosts(V1)) shouldBe Right(2317)

    val exprV2 = ExpressionCompiler(ctxV2.compilerContext, getAllFuncExpression(V2)).explicitGet()._1
    estimate(exprV2, ctxV2, utils.functionCosts(V2)) shouldBe Right(2317)

    val exprV3 = ExpressionCompiler(ctxV3.compilerContext, getAllFuncExpression(V3)).explicitGet()._1
    estimate(exprV3, ctxV3, utils.functionCosts(V3)) shouldBe Right(1882)
  }
}
