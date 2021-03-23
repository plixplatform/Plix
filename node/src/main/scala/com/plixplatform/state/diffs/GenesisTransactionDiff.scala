package com.plixplatform.state.diffs

import com.plixplatform.lang.ValidationError
import com.plixplatform.state.{Diff, LeaseBalance, Portfolio}
import com.plixplatform.transaction.TxValidationError.GenericError
import com.plixplatform.transaction.GenesisTransaction

import scala.util.{Left, Right}

object GenesisTransactionDiff {
  def apply(height: Int)(tx: GenesisTransaction): Either[ValidationError, Diff] = {
    if (height != 1) Left(GenericError("GenesisTransaction cannot appear in non-initial block"))
    else
      Right(Diff(height = height, tx = tx, portfolios = Map(tx.recipient -> Portfolio(balance = tx.amount, LeaseBalance.empty, assets = Map.empty))))
  }
}
