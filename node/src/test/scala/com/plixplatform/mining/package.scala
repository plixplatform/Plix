package com.plixplatform

import com.plixplatform.state.{Blockchain, Diff}
import com.plixplatform.transaction.Transaction

package object mining {
  private[mining] def createConstConstraint(maxSize: Long, transactionSize: => Long) = OneDimensionalMiningConstraint(
    maxSize,
    new com.plixplatform.mining.TxEstimators.Fn {
      override def apply(b: Blockchain, t: Transaction, d: Diff) = transactionSize
      override val minEstimate                          = transactionSize
      override def toString(): String = s"const($transactionSize)"
    }
  )
}
