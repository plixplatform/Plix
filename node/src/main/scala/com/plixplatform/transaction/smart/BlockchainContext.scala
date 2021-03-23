package com.plixplatform.transaction.smart

import cats.kernel.Monoid
import com.plixplatform.common.state.ByteStr
import com.plixplatform.lang.directives.DirectiveSet
import com.plixplatform.lang.directives.values.{ContentType, ScriptType, StdLibVersion}
import com.plixplatform.lang.v1.evaluator.ctx.EvaluationContext
import com.plixplatform.lang.v1.evaluator.ctx.impl.plix.PlixContext
import com.plixplatform.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.plixplatform.lang.{ExecutionError, Global}
import com.plixplatform.state._
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
