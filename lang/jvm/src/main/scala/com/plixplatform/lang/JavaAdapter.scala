package com.plixlatform.lang

import cats.kernel.Monoid
import com.plixlatform.lang.directives.values.V2
import com.plixlatform.lang.v1.compiler.ExpressionCompiler
import com.plixlatform.lang.v1.compiler.Terms.EXPR
import com.plixlatform.lang.v1.evaluator.ctx.impl.plix.PlixContext
import com.plixlatform.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}

object JavaAdapter {
  private val version = V2

  lazy val ctx =
    Monoid.combineAll(
      Seq(
        CryptoContext.compilerContext(Global, version),
        PlixContext.build(???, null).compilerContext,
        PureContext.build(Global, version).compilerContext
      ))

  def compile(input: String): EXPR = {
    ExpressionCompiler
      .compile(input, ctx)
      .fold(
        error => throw new IllegalArgumentException(error),
        expr => expr
      )
  }
}
