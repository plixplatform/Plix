package com.plixlatform

import com.plixlatform.state.{Blockchain, Diff}
import com.plixlatform.transaction.Transaction

package object mining {
  private[mining] def createConstConstraint(maxSize: Long, transactionSize: => Long) = OneDimensionalMiningConstraint(
    maxSize,
    new com.plixlatform.mining.TxEstimators.Fn {
      override def apply(b: Blockchain, t: Transaction, d: Diff) = transactionSize
      override val minEstimate                          = transactionSize
      override def toString(): String = s"const($transactionSize)"
    }
  )
}
