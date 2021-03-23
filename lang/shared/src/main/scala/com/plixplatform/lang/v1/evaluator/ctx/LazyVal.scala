package com.plixlatform.lang.v1.evaluator.ctx

import cats.Eval
import cats.implicits._
import com.plixlatform.lang.{ExecutionError, TrampolinedExecResult}
import com.plixlatform.lang.v1.compiler.Terms.EVALUATED
import com.plixlatform.lang.v1.evaluator.LogCallback

sealed trait LazyVal {
  val value: Eval[Either[ExecutionError,EVALUATED]]
}

object LazyVal {
  private case class LazyValImpl(v: Eval[Either[ExecutionError,EVALUATED]], lc: LogCallback) extends LazyVal {
    override val value: Eval[Either[ExecutionError,EVALUATED]] =
        v.flatTap(a => Eval.now(lc(a))).memoize
  }

  def apply(v: TrampolinedExecResult[EVALUATED], lc: LogCallback = _ => ()): LazyVal =
    LazyValImpl(v.value, lc)
}
