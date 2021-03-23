package com.plixlatform.transaction.smart

import cats.kernel.Monoid
import com.plixlatform.common.state.ByteStr
import com.plixlatform.lang.directives.DirectiveSet
import com.plixlatform.lang.directives.values.{ContentType, ScriptType, StdLibVersion}
import com.plixlatform.lang.v1.evaluator.ctx.EvaluationContext
import com.plixlatform.lang.v1.evaluator.ctx.impl.plix.PlixContext
import com.plixlatform.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.plixlatform.lang.{ExecutionError, Global}
import com.plixlatform.state._
import monix.eval.Coeval

object BlockchainContext {

  type In = PlixEnvironment.In
  def build(version: StdLibVersion,
            nByte: Byte,
            in: Coeval[In],
            h: Coeval[Int],
            blockchain: Blockchain,
            isTokenContext: Boolean,
            isContract: Boolean,
            address: Coeval[ByteStr]): Either[ExecutionError, EvaluationContext] =
    DirectiveSet(
      version,
      ScriptType.isAssetScript(isTokenContext),
      ContentType.isDApp(isContract)
    ).map(PlixContext.build(_, new PlixEnvironment(nByte, in, h, blockchain, address)))
      .map(Seq(PureContext.build(Global, version), CryptoContext.build(Global, version), _))
      .map(Monoid.combineAll(_))
      .map(_.evaluationContext)
}
