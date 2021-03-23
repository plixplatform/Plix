package com.plixlatform.transaction.smart.script

import com.plixlatform.lang.directives.Directive.extractValue
import com.plixlatform.lang.directives.DirectiveKey._
import com.plixlatform.lang.directives._
import com.plixlatform.lang.directives.values._
import com.plixlatform.lang.script.ContractScript._
import com.plixlatform.lang.script.v1.ExprScript
import com.plixlatform.lang.script.{ContractScript, Script}
import com.plixlatform.lang.utils._
import com.plixlatform.lang.v1.ScriptEstimator
import com.plixlatform.lang.v1.compiler.{ContractCompiler, ExpressionCompiler}
import com.plixlatform.utils._

object ScriptCompiler extends ScorexLogging {

  @Deprecated
  def apply(scriptText: String, isAssetScript: Boolean): Either[String, (Script, Long)] = {
    for {
      directives <- DirectiveParser(scriptText)
      contentType = extractValue(directives, CONTENT_TYPE)
      version     = extractValue(directives, STDLIB_VERSION)
      scriptType  = if (isAssetScript) Asset else Account
      _      <- DirectiveSet(version, scriptType, contentType)
      script <- tryCompile(scriptText, contentType, version, isAssetScript)
    } yield (script, script.complexity)
  }

  def compile(scriptText: String): Either[String, (Script, Long)] = {
    for {
      directives <- DirectiveParser(scriptText)
      result     <- apply(scriptText, extractValue(directives, SCRIPT_TYPE) == Asset)
    } yield result
  }

  private def tryCompile(src: String, cType: ContentType, version: StdLibVersion, isAssetScript: Boolean): Either[String, Script] = {
    val ctx = compilerContext(version, cType, isAssetScript)
    try {
      cType match {
        case Expression => ExpressionCompiler.compile(src, ctx).flatMap(expr => ExprScript.apply(version, expr))
        case DApp       => ContractCompiler.compile(src, ctx).flatMap(expr => ContractScript.apply(version, expr))
      }
    } catch {
      case ex: Throwable =>
        log.error("Error compiling script", ex)
        log.error(src)
        val msg = Option(ex.getMessage).getOrElse("Parsing failed: Unknown error")
        Left(msg)
    }
  }

  def estimate(script: Script, version: StdLibVersion): Either[String, Long] = script match {
    case s: ExprScript         => ScriptEstimator(varNames(version, Expression), functionCosts(version), s.expr)
    case s: ContractScriptImpl => ContractScript.estimateComplexity(version, s.expr).map(_._1)
    case _                     => ???
  }

}
