package com.plixplatform.mining
import com.plixplatform.features.BlockchainFeatures
import com.plixplatform.features.FeatureProvider._
import com.plixplatform.state.{Blockchain, Diff}
import com.plixplatform.transaction.Transaction
import com.plixplatform.utils.ScorexLogging

import scala.annotation.elidable

// TODO: Remove when tested
private[mining] object ScriptRunsLegacy extends ScorexLogging {
  // Can be removed from resulting build with -Xelide-below 2001
  @elidable(elidable.ASSERTION)
  def assertEquals(blockchain: Blockchain, tx: Transaction, diff: Diff): Unit = {
    val ride4Dapps = {
      val height = diff.transactions
        .get(tx.id())
        .map(_._1)
        .getOrElse(blockchain.height)

      blockchain.isFeatureActivated(BlockchainFeatures.Ride4DApps, height)
    }

    lazy val oldRulesRuns: Int = calculateForTransaction(blockchain, tx)
    assert(ride4Dapps || diff.scriptsRun == oldRulesRuns, s"$tx script runs ${diff.scriptsRun} not equals to legacy $oldRulesRuns")
  }

  private[this] def calculateForTransaction(blockchain: Blockchain, tx: Transaction): Int = {
    import com.plixplatform.transaction.Asset.IssuedAsset
    import com.plixplatform.transaction.assets.exchange.ExchangeTransaction
    import com.plixplatform.transaction.assets.{BurnTransaction, ReissueTransaction, SponsorFeeTransaction}
    import com.plixplatform.transaction.smart.InvokeScriptTransaction
    import com.plixplatform.transaction.transfer.{MassTransferTransaction, TransferTransaction}
    import com.plixplatform.transaction.{Authorized, Transaction}

    val smartAccountRun = tx match {
      case x: Transaction with Authorized if blockchain.hasScript(x.sender) => 1
      case _                                                                => 0
    }

    val assetIds = tx match {
      case x: TransferTransaction     => x.assetId.fold[Seq[IssuedAsset]](Nil)(Seq(_))
      case x: MassTransferTransaction => x.assetId.fold[Seq[IssuedAsset]](Nil)(Seq(_))
      case x: BurnTransaction         => Seq(x.asset)
      case x: ReissueTransaction      => Seq(x.asset)
      case x: SponsorFeeTransaction   => Seq(x.asset)
      case x: ExchangeTransaction =>
        Seq(
          x.buyOrder.assetPair.amountAsset.fold[Seq[IssuedAsset]](Nil)(Seq(_)),
          x.buyOrder.assetPair.priceAsset.fold[Seq[IssuedAsset]](Nil)(Seq(_))
        ).flatten
      case _ => Seq.empty
    }
    val smartTokenRuns = assetIds.flatMap(blockchain.assetDescription).count(_.script.isDefined)

    val invokeScriptRun = tx match {
      case tx: InvokeScriptTransaction => 1
      case _                           => 0
    }

    smartAccountRun + smartTokenRuns + invokeScriptRun
  }
}
