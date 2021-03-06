package com.plixplatform.transaction
import monix.eval.Coeval

trait Proven extends Authorized {
  def proofs: Proofs
  val bodyBytes: Coeval[Array[Byte]]
}
