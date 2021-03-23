package com.plixplatform.state.diffs

import com.plixplatform.features.BlockchainFeatures
import com.plixplatform.features.FeatureProvider._
import com.plixplatform.lang.ValidationError
import com.plixplatform.state.{Blockchain, Diff, LeaseBalance, Portfolio}
import com.plixplatform.transaction.CreateAliasTransaction
import com.plixplatform.transaction.TxValidationError.GenericError

import scala.util.Right

object CreateAliasTransactionDiff {
  def apply(blockchain: Blockchain, height: Int)(tx: CreateAliasTransaction): Either[ValidationError, Diff] =
    if (blockchain.isFeatureActivated(BlockchainFeatures.DataTransaction, height) && !blockchain.canCreateAlias(tx.alias))
      Left(GenericError("Alias already claimed"))
    else
      Right(
        Diff(
          height = height,
          tx = tx,
          portfolios = Map(tx.sender.toAddress -> Portfolio(-tx.fee, LeaseBalance.empty, Map.empty)),
          aliases = Map(tx.alias -> tx.sender.toAddress),
          scriptsRun = DiffsCommon.countScriptRuns(blockchain, tx),
          scriptsComplexity = DiffsCommon.countScriptsComplexity(blockchain, tx)
        ))
}
