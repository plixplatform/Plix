package com.plixplatform.transaction

import com.plixplatform.common.state.ByteStr
import com.plixplatform.crypto
import monix.eval.Coeval

trait FastHashId extends ProvenTransaction {
  val id: Coeval[ByteStr] = Coeval.evalOnce(FastHashId.create(this.bodyBytes()))
}

object FastHashId {
  def create(bodyBytes: Array[Byte]): ByteStr = {
    ByteStr(crypto.fastHash(bodyBytes))
  }
}
