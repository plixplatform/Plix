package com.plixlatform.transaction.smart.script

import cats.implicits._
import com.plixlatform.account.AddressScheme
import com.plixlatform.common.state.ByteStr
import com.plixlatform.lang._
import com.plixlatform.lang.contract.DApp
import com.plixlatform.lang.script.v1.ExprScript
import com.plixlatform.lang.script.{ContractScript, Script}
import com.plixlatform.lang.v1.compiler.Terms.{EVALUATED, TRUE}
import com.plixlatform.lang.v1.evaluator.{EvaluatorV1, _}
import com.plixlatform.state._
import com.plixlatform.transaction.TxValidationError.GenericError
import com.plixlatform.transaction.smart.{BlockchainContext, RealTransactionWrapper, Verifier}
import com.plixlatform.transaction.{Authorized, Proven}
import monix.eval.Coeval

object ScriptRunner {
  type TxOrd = BlockchainContext.In

  def apply(height: Int,
            in: TxOrd,
            blockchain: Blockchain,
            script: Script,
            isAssetScript: Boolean,
            scriptContainerAddress: ByteStr): (Log, Either[ExecutionError, EVALUATED]) = {
    script match {
      case s: ExprScript =>
        val ctx = BlockchainContext.build(
          script.stdLibVersion,
          AddressScheme.current.chainId,
          Coeval.evalOnce(in),
          Coeval.evalOnce(height),
          blockchain,
          isAssetScript,
          isContract = false,
          Coeval(scriptContainerAddress)
        )
        EvaluatorV1.applyWithLogging[EVALUATED](ctx, s.expr)
      case ContractScript.ContractScriptImpl(_, DApp(_, decls, _, Some(vf)), _) =>
        val ctx = BlockchainContext.build(
          script.stdLibVersion,
          AddressScheme.current.chainId,
          Coeval.evalOnce(in),
          Coeval.evalOnce(height),
          blockchain,
          isAssetScript,
          isContract = true,
          Coeval(scriptContainerAddress)
        )
        val evalContract = in.eliminate(
          t => ContractEvaluator.verify(decls, vf, RealTransactionWrapper.apply(t)),
          _.eliminate(t => ContractEvaluator.verify(decls, vf, RealTransactionWrapper.ord(t)), _ => ???)
        )
        EvaluatorV1.evalWithLogging(ctx, evalContract)

      case ContractScript.ContractScriptImpl(_, DApp(_, _, _, None), _) =>
        val t: Proven with Authorized =
          in.eliminate(_.asInstanceOf[Proven with Authorized], _.eliminate(_.asInstanceOf[Proven with Authorized], _ => ???))
        (List.empty, Verifier.verifyAsEllipticCurveSignature[Proven with Authorized](t) match {
          case Right(_)                => Right(TRUE)
          case Left(GenericError(err)) => Left(err)
        })
      case _ => (List.empty, "Unsupported script version".asLeft[EVALUATED])
    }
  }
}
