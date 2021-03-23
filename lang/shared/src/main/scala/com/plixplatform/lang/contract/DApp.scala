package com.plixlatform.lang.contract

import com.plixlatform.common.state.ByteStr
import com.plixlatform.lang.contract.DApp.{CallableFunction, VerifierFunction}
import com.plixlatform.lang.v1.compiler.CompilationError.Generic
import com.plixlatform.lang.v1.compiler.Terms.DECLARATION
import com.plixlatform.lang.v1.compiler.Types._
import com.plixlatform.lang.v1.compiler.{CompilationError, Terms}
import com.plixlatform.lang.v1.evaluator.ctx.impl.plix.PlixContext

case class DApp(
    meta: ByteStr,
    decs: List[DECLARATION],
    callableFuncs: List[CallableFunction],
    verifierFuncOpt: Option[VerifierFunction]
)

object DApp {

  sealed trait Annotation {
    def invocationArgName: String
    def dic: Map[String, FINAL]
  }
  object Annotation {
    def parse(name: String, args: List[String]): Either[CompilationError, Annotation] = {
      (name, args) match {
        case ("Verifier", s :: Nil) => Right(VerifierAnnotation(s))
        case ("Verifier", s :: xs)  => Left(Generic(0, 0, "Incorrect amount of bound args in Verifier, should be one, e.g. @Verifier(tx)"))
        case ("Callable", s :: Nil) => Right(CallableAnnotation(s))
        case ("Callable", s :: xs)  => Left(Generic(0, 0, "Incorrect amount of bound args in Callable, should be one, e.g. @Callable(inv)"))
        case _                      => Left(Generic(0, 0, "Annotation not recognized"))
      }
    }

    def validateAnnotationSet(l: List[Annotation]): Either[CompilationError, Unit] = {
      l match {
        case (v: VerifierAnnotation) :: Nil => Right(())
        case (c: CallableAnnotation) :: Nil => Right(())
        case _                              => Left(Generic(0, 0, "Unsupported annotation set"))
      }
    }
  }
  case class CallableAnnotation(invocationArgName: String) extends Annotation {
    lazy val dic: Map[String, CASETYPEREF] = Map(invocationArgName -> com.plixlatform.lang.v1.evaluator.ctx.impl.plix.Types.invocationType)
  }
  case class VerifierAnnotation(invocationArgName: String) extends Annotation {
    lazy val dic: Map[String, UNION] = Map(invocationArgName -> PlixContext.verifierInput)
  }

  sealed trait AnnotatedFunction {
    def annotation: Annotation
    def u: Terms.FUNC
  }
  case class CallableFunction(override val annotation: CallableAnnotation, override val u: Terms.FUNC) extends AnnotatedFunction
  case class VerifierFunction(override val annotation: VerifierAnnotation, override val u: Terms.FUNC) extends AnnotatedFunction
}
