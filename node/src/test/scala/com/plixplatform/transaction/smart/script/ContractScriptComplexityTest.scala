package com.plixlatform.transaction.smart.script

import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.common.state.ByteStr
import com.plixlatform.lang.contract.DApp
import com.plixlatform.lang.contract.DApp._
import com.plixlatform.lang.directives.values._
import com.plixlatform.lang.script.ContractScript
import com.plixlatform.lang.v1.compiler.Terms
import com.plixlatform.lang.v1.compiler.Terms._
import com.plixlatform.lang.v1.evaluator.ctx.impl.PureContext._
import com.plixlatform.lang.v1.testing.TypedScriptGen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class ContractScriptComplexityTest extends PropSpec with PropertyChecks with Matchers with TypedScriptGen {

  property("estimate contract script correctly") {
    val contract = DApp(
      ByteStr.empty,
      List.empty,
      List(
        CallableFunction(
          CallableAnnotation(""),
          Terms.FUNC(
            "first",
            List("arg1", "arg2"),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              REF("x")
            )
          )
        ),
        CallableFunction(
          CallableAnnotation(""),
          Terms.FUNC(
            "default",
            List(),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              REF("x")
            )
          )
        )
      ),
      Some(
        VerifierFunction(
          VerifierAnnotation(""),
          Terms.FUNC(
            "third",
            List("arg1", "arg2"),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              LET_BLOCK(
                LET("y", FUNCTION_CALL(sumLong.header, List(REF("x"), CONST_LONG(1)))),
                REF("y")
              )
            )
          )
        )
      )
    )

    ContractScript.estimateComplexity(V3, contract) shouldBe Right((41, Vector(("first", 32), ("default", 20), ("third", 41))))
  }

  property("estimate contract script with context correctly") {
    val contract = DApp(
      ByteStr.empty,
      List(
        LET("y", FUNCTION_CALL(sumString.header, List(CONST_STRING("a").explicitGet(), CONST_STRING("b").explicitGet()))),
        LET("z", FUNCTION_CALL(sumString.header, List(CONST_STRING("c").explicitGet(), CONST_STRING("d").explicitGet())))
      ),
      List(
        CallableFunction(
          CallableAnnotation(""),
          Terms.FUNC(
            "first",
            List("arg1", "arg2"),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(REF("y"), REF("z")))),
              REF("x")
            )
          )
        ),
        CallableFunction(
          CallableAnnotation(""),
          Terms.FUNC(
            "default",
            List(),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              REF("x")
            )
          )
        )
      ),
      Some(
        VerifierFunction(
          VerifierAnnotation(""),
          Terms.FUNC(
            "third",
            List("arg1", "arg2"),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              LET_BLOCK(
                LET("y", FUNCTION_CALL(sumLong.header, List(REF("x"), CONST_LONG(1)))),
                REF("y")
              )
            )
          )
        )
      )
    )

    ContractScript.estimateComplexity(V3, contract) shouldBe Right((68, Vector(("first", 68), ("default", 30), ("third", 51))))
  }

  property("estimate contract script with context correctly 2") {
    val contract = DApp(
      ByteStr.empty,
      List(
        LET("y", FUNCTION_CALL(sumString.header, List(CONST_STRING("a").explicitGet(), CONST_STRING("b").explicitGet()))),
        LET("z", FUNCTION_CALL(sumString.header, List(CONST_STRING("c").explicitGet(), CONST_STRING("d").explicitGet())))
      ),
      List(
        CallableFunction(
          CallableAnnotation(""),
          Terms.FUNC(
            "first",
            List("arg1", "arg2"),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(REF("y"), REF("z")))),
              REF("x")
            )
          )
        ),
        CallableFunction(
          CallableAnnotation(""),
          Terms.FUNC(
            "default",
            List(),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              REF("x")
            )
          )
        )
      ),
      Some(
        VerifierFunction(
          VerifierAnnotation(""),
          Terms.FUNC(
            "second",
            List("arg1", "arg2"),
            LET_BLOCK(
              LET("x", FUNCTION_CALL(sumLong.header, List(CONST_LONG(3), CONST_LONG(0)))),
              LET_BLOCK(
                LET("y", FUNCTION_CALL(sumLong.header, List(REF("x"), CONST_LONG(1)))),
                REF("y")
              )
            )
          )
        )
      )
    )

    ContractScript.estimateComplexity(V3, contract) shouldBe Right((68, Vector(("first", 68), ("default", 30), ("second", 51))))
  }
}
