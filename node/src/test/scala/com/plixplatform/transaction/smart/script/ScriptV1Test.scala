package com.plixplatform.transaction.smart.script

import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.lang.v1.FunctionHeader
import com.plixplatform.lang.v1.compiler.Terms._
import com.plixplatform.lang.v1.evaluator.FunctionIds._
import com.plixplatform.lang.v1.evaluator.ctx.impl.PureContext
import com.plixplatform.lang.v1.testing.TypedScriptGen
import com.plixplatform.state.diffs._
import com.plixplatform.lang.script.v1.ExprScript
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class ScriptV1Test extends PropSpec with PropertyChecks with Matchers with TypedScriptGen {

  property("ScriptV1.apply should permit BOOLEAN scripts") {
    forAll(BOOLEANgen(10)) { expr =>
      ExprScript(expr) shouldBe 'right
    }
  }

  property("ScriptV1.apply should deny too complex scripts") {
    val byteStr = CONST_BYTESTR(ByteStr.fromBytes(1)).explicitGet()
    val expr = (1 to 21)
      .map { _ =>
        FUNCTION_CALL(
          function = FunctionHeader.Native(SIGVERIFY),
          args = List(byteStr, byteStr, byteStr)
        )
      }
      .reduceLeft[EXPR](IF(_, _, FALSE))

    ExprScript(expr) should produce("Script is too complex")
  }

  property("ScriptV1.apply should deny too big scripts") {
    val bigSum = (1 to 100).foldLeft[EXPR](CONST_LONG(0)) { (r, i) =>
      FUNCTION_CALL(
        function = FunctionHeader.Native(SUM_LONG),
        args = List(r, CONST_LONG(i))
      )
    }
    val expr = (1 to 9).foldLeft[EXPR](CONST_LONG(0)) { (r, i) =>
      FUNCTION_CALL(
        function = PureContext.eq.header,
        args = List(r, bigSum)
      )
    }

    ExprScript(expr) should produce("Script is too large")
  }

  property("19 sigVerify should fit in maxSizeInBytes") {
    val byteStr = CONST_BYTESTR(ByteStr.fromBytes(1)).explicitGet()
    val expr = (1 to 19)
      .map { _ =>
        FUNCTION_CALL(
          function = FunctionHeader.Native(SIGVERIFY),
          args = List(byteStr, byteStr, byteStr)
        )
      }
      .reduceLeft[EXPR](IF(_, _, FALSE))

    ExprScript(expr) shouldBe 'right
  }

  property("Expression block version check - successful on very deep expressions(stack overflow check)") {
    val expr = (1 to 100000).foldLeft[EXPR](CONST_LONG(0)) { (acc, _) =>
      FUNCTION_CALL(FunctionHeader.Native(SUM_LONG), List(CONST_LONG(1), acc))
    }

    com.plixplatform.lang.v1.compiler.??ontainsBlockV2(expr) shouldBe false
  }

}
