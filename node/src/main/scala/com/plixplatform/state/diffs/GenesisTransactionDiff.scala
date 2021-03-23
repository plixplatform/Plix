package com.plixlatform.state.diffs

import com.plixlatform.lang.ValidationError
import com.plixlatform.state.{Diff, LeaseBalance, Portfolio}
import com.plixlatform.transaction.TxValidationError.GenericError
import com.plixlatform.transaction.GenesisTransaction

import scala.util.{Left, Right}

object GenesisTransactionDiff {
  def apply(height: Int)(tx: GenesisTransaction): Either[ValidationError, Diff] = {
    if (height != 1) Left(GenericError("GenesisTransaction cannot appear in non-initial block"))
    else
      Right(Diff(height = height, tx = tx, portfolios = Map(tx.recipient -> Portfolio(balance = tx.amount, LeaseBalance.empty, assets = Map.empty))))
  }
}
