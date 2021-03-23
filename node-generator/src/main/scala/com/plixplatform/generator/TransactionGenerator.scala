package com.plixplatform.generator

import com.plixplatform.transaction.Transaction

trait TransactionGenerator extends Iterator[Iterator[Transaction]] {
  override val hasNext = true
}
