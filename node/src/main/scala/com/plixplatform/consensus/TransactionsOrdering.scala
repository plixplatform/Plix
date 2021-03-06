package com.plixplatform.consensus

import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.Transaction

object TransactionsOrdering {
  trait PlixOrdering extends Ordering[Transaction] {
    def txTimestampOrder(ts: Long): Long
    private def orderBy(t: Transaction): (Double, Long, Long) = {
      val size        = t.bytes().length
      val byFee       = if (t.assetFee._1 != Plix) 0 else -t.assetFee._2
      val byTimestamp = txTimestampOrder(t.timestamp)

      (byFee.toDouble / size.toDouble, byFee, byTimestamp)
    }
    override def compare(first: Transaction, second: Transaction): Int = {
      implicitly[Ordering[(Double, Long, Long)]].compare(orderBy(first), orderBy(second))
    }
  }

  object InBlock extends PlixOrdering {
    // sorting from network start
    override def txTimestampOrder(ts: Long): Long = -ts
  }

  object InUTXPool extends PlixOrdering {
    override def txTimestampOrder(ts: Long): Long = ts
  }
}
