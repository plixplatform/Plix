package com.plixplatform.lang

import com.plixplatform.lang.directives.values._
import com.plixplatform.lang.v1.evaluator.ctx.impl.plix.Types
import org.scalatest.{FreeSpec, Matchers}

class ContextVersionTest extends FreeSpec with Matchers {

  "InvokeScriptTransaction" - {
    "exist in lib version 3" in {
      val types = Types.buildPlixTypes(proofsEnabled = true, V3)
      types.count(c => c.name == "InvokeScriptTransaction") shouldEqual 1
    }

    "doesn't exist in lib version 2" in {
      val types = Types.buildPlixTypes(proofsEnabled = true, V2)
      types.count(c => c.name == "InvokeScriptTransaction") shouldEqual 0
    }
  }
}
