package com.plixplatform.transaction.smart.script

import cats.kernel.Monoid
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.lang.directives.DirectiveSet
import com.plixplatform.lang.directives.values._
import com.plixplatform.lang.{Global, utils}
import com.plixplatform.lang.v1.FunctionHeader.User
import com.plixplatform.lang.v1.compiler.Terms._
import com.plixplatform.lang.v1.evaluator.ctx.impl.plix._
import com.plixplatform.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.plixplatform.lang.v1.testing.TypedScriptGen
import com.plixplatform.lang.v1.{CTX, FunctionHeader, ScriptEstimator}
import com.plixplatform.transaction.smart.PlixEnvironment
import com.plixplatform.utils.EmptyBlockchain
import monix.eval.Coeval
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class UserFunctionComplexityTest extends PropSpec with PropertyChecks with Matchers with TypedScriptGen {

  private def estimate(expr: EXPR, ctx: CTX, funcCosts: Map[FunctionHeader, Coeval[Long]]): Either[String, Long] = {
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
            new PlixEnvironment('T'.toByte, Coeval(???), Coeval(???), EmptyBlockchain, Coeval(???)),
          )
        ))
  }
  private val funcCostsV1 = utils.functionCosts(V1)

  property("estimate script for stdLib V1 with UserFunctions") {

    def est: EXPR => Either[String, Long] = estimate(_, ctxV1, funcCostsV1)

    val exprNe = FUNCTION_CALL(PureContext.ne, List(CONST_LONG(1), CONST_LONG(2)))
    est(exprNe).explicitGet() shouldBe 28

    val exprThrow = FUNCTION_CALL(PureContext.throwNoMessage, List())
    est(exprThrow).explicitGet() shouldBe 2

    val exprExtract = LET_BLOCK(
      LET("x", CONST_LONG(2)),
      FUNCTION_CALL(PureContext.extract, List(REF("x")))
    )
    est(exprExtract).explicitGet() shouldBe 21

    val exprIsDefined = LET_BLOCK(
      LET("x", CONST_LONG(2)),
      FUNCTION_CALL(PureContext.isDefined, List(REF("x")))
    )
    est(exprIsDefined).explicitGet() shouldBe 43

    val exprDropRightBytes = FUNCTION_CALL(PureContext.dropRightBytes, List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet(), CONST_LONG(1)))
    est(exprDropRightBytes).explicitGet() shouldBe 21

    val exprTakeRightBytes = FUNCTION_CALL(PureContext.takeRightBytes, List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet(), CONST_LONG(1)))
    est(exprTakeRightBytes).explicitGet() shouldBe 21

    val exprDropRightString = FUNCTION_CALL(PureContext.dropRightString, List(CONST_STRING("str").explicitGet(), CONST_LONG(1)))
    est(exprDropRightString).explicitGet() shouldBe 21

    val exprTakeRightString = FUNCTION_CALL(PureContext.takeRightString, List(CONST_STRING("str").explicitGet(), CONST_LONG(1)))
    est(exprTakeRightString).explicitGet() shouldBe 21

    val exprUMinus = FUNCTION_CALL(PureContext.uMinus, List(CONST_LONG(1)))
    est(exprUMinus).explicitGet() shouldBe 10

    val exprUNot = FUNCTION_CALL(PureContext.uNot, List(TRUE))
    est(exprUNot).explicitGet() shouldBe 12

    val exprAddressFromPublicKey = FUNCTION_CALL(User("addressFromPublicKey"), List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet()))
    est(exprAddressFromPublicKey).explicitGet() shouldBe 83

    val exprAddressFromString = FUNCTION_CALL(User("addressFromString"), List(CONST_STRING("address").explicitGet()))
    est(exprAddressFromString).explicitGet() shouldBe 125

    val exprPlixBalance = FUNCTION_CALL(User("plixBalance"), List(CONST_STRING("alias").explicitGet()))
    est(exprPlixBalance).explicitGet() shouldBe 110
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
            new PlixEnvironment('T'.toByte, Coeval(???), Coeval(???), EmptyBlockchain, Coeval(???))
          )
        ))
  }
  private val funcCostsV2 = utils.functionCosts(V2)

  property("estimate script for stdLib V2 with UserFunctions") {

    def est: EXPR => Either[String, Long] = estimate(_, ctxV2, funcCostsV2)

    val exprNe = FUNCTION_CALL(PureContext.ne, List(CONST_LONG(1), CONST_LONG(2)))
    est(exprNe).explicitGet() shouldBe 28

    val exprThrow = FUNCTION_CALL(PureContext.throwNoMessage, List())
    est(exprThrow).explicitGet() shouldBe 2

    val exprExtract = LET_BLOCK(
      LET("x", CONST_LONG(2)),
      FUNCTION_CALL(PureContext.extract, List(REF("x")))
    )
    est(exprExtract).explicitGet() shouldBe 21

    val exprIsDefined = LET_BLOCK(
      LET("x", CONST_LONG(2)),
      FUNCTION_CALL(PureContext.isDefined, List(REF("x")))
    )
    est(exprIsDefined).explicitGet() shouldBe 43

    val exprDropRightBytes = FUNCTION_CALL(PureContext.dropRightBytes, List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet(), CONST_LONG(1)))
    est(exprDropRightBytes).explicitGet() shouldBe 21

    val exprTakeRightBytes = FUNCTION_CALL(PureContext.takeRightBytes, List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet(), CONST_LONG(1)))
    est(exprTakeRightBytes).explicitGet() shouldBe 21

    val exprDropRightString = FUNCTION_CALL(PureContext.dropRightString, List(CONST_STRING("str").explicitGet(), CONST_LONG(1)))
    est(exprDropRightString).explicitGet() shouldBe 21

    val exprTakeRightString = FUNCTION_CALL(PureContext.takeRightString, List(CONST_STRING("str").explicitGet(), CONST_LONG(1)))
    est(exprTakeRightString).explicitGet() shouldBe 21

    val exprUMinus = FUNCTION_CALL(PureContext.uMinus, List(CONST_LONG(1)))
    est(exprUMinus).explicitGet() shouldBe 10

    val exprUNot = FUNCTION_CALL(PureContext.uNot, List(TRUE))
    est(exprUNot).explicitGet() shouldBe 12

    val exprAddressFromPublicKey = FUNCTION_CALL(User("addressFromPublicKey"), List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet()))
    est(exprAddressFromPublicKey).explicitGet() shouldBe 83

    val exprAddressFromString = FUNCTION_CALL(User("addressFromString"), List(CONST_STRING("address").explicitGet()))
    est(exprAddressFromString).explicitGet() shouldBe 125

    val exprPlixBalance = FUNCTION_CALL(User("plixBalance"), List(CONST_STRING("alias").explicitGet()))
    est(exprPlixBalance).explicitGet() shouldBe 110
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
            new PlixEnvironment('T'.toByte, Coeval(???), Coeval(???), EmptyBlockchain, Coeval(???))
          )
        ))
  }
  private val funcCostsV3 = utils.functionCosts(V3)

  property("estimate script for stdLib V3 with UserFunctions") {

    def est: EXPR => Either[String, Long] = estimate(_, ctxV3, funcCostsV3)

    val exprNe = FUNCTION_CALL(PureContext.ne, List(CONST_LONG(1), CONST_LONG(2)))
    est(exprNe).explicitGet() shouldBe 3

    val exprThrow = FUNCTION_CALL(PureContext.throwNoMessage, List())
    est(exprThrow).explicitGet() shouldBe 1

    val exprExtract = LET_BLOCK(
      LET("x", CONST_LONG(2)),
      FUNCTION_CALL(PureContext.extract, List(REF("x")))
    )
    est(exprExtract).explicitGet() shouldBe 21

    val exprIsDefined = LET_BLOCK(
      LET("x", CONST_LONG(2)),
      FUNCTION_CALL(PureContext.isDefined, List(REF("x")))
    )
    est(exprIsDefined).explicitGet() shouldBe 9

    val exprDropRightBytes = FUNCTION_CALL(PureContext.dropRightBytes, List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet(), CONST_LONG(1)))
    est(exprDropRightBytes).explicitGet() shouldBe 21

    val exprTakeRightBytes = FUNCTION_CALL(PureContext.takeRightBytes, List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet(), CONST_LONG(1)))
    est(exprTakeRightBytes).explicitGet() shouldBe 21

    val exprDropRightString = FUNCTION_CALL(PureContext.dropRightString, List(CONST_STRING("str").explicitGet(), CONST_LONG(1)))
    est(exprDropRightString).explicitGet() shouldBe 21

    val exprTakeRightString = FUNCTION_CALL(PureContext.takeRightString, List(CONST_STRING("str").explicitGet(), CONST_LONG(1)))
    est(exprTakeRightString).explicitGet() shouldBe 21

    val exprUMinus = FUNCTION_CALL(PureContext.uMinus, List(CONST_LONG(1)))
    est(exprUMinus).explicitGet() shouldBe 2

    val exprUNot = FUNCTION_CALL(PureContext.uNot, List(TRUE))
    est(exprUNot).explicitGet() shouldBe 2

    val exprDataByIndex = LET_BLOCK(
      LET("arr", FUNCTION_CALL(PureContext.listConstructor, List(CONST_STRING("str_1").explicitGet(), REF("nil")))),
      FUNCTION_CALL(User("getString"), List(REF("arr"), CONST_LONG(0)))
    )
    est(exprDataByIndex).explicitGet() shouldBe 43

    val exprAddressFromPublicKey = FUNCTION_CALL(User("addressFromPublicKey"), List(CONST_BYTESTR(ByteStr.fromLong(2)).explicitGet()))
    est(exprAddressFromPublicKey).explicitGet() shouldBe 83

    val exprAddressFromString = FUNCTION_CALL(User("addressFromString"), List(CONST_STRING("address").explicitGet()))
    est(exprAddressFromString).explicitGet() shouldBe 125

    val exprPlixBalance = FUNCTION_CALL(User("plixBalance"), List(CONST_STRING("alias").explicitGet()))
    est(exprPlixBalance).explicitGet() shouldBe 110
  }
}
