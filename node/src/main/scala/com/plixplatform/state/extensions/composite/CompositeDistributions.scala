package com.plixlatform.state.extensions.composite

import cats.kernel.Monoid
import com.plixlatform.account.Address
import com.plixlatform.lang.ValidationError
import com.plixlatform.state.extensions.Distributions
import com.plixlatform.state.{AssetDistribution, AssetDistributionPage, Blockchain, Diff, Portfolio}
import com.plixlatform.transaction.Asset.IssuedAsset
import com.plixlatform.transaction.assets.IssueTransaction
import monix.reactive.Observable

private[state] final class CompositeDistributions(blockchain: Blockchain, baseProvider: Distributions, getDiff: () => Option[Diff])
    extends Distributions {
  override def portfolio(a: Address): Portfolio = {
    val p = getDiff().fold(Portfolio.empty)(_.portfolios.getOrElse(a, Portfolio.empty))
    Monoid.combine(baseProvider.portfolio(a), p)
  }

  override def nftObservable(address: Address, from: Option[IssuedAsset]): Observable[IssueTransaction] =
    com.plixlatform.state.nftListFromDiff(blockchain, baseProvider, getDiff())(address, from)

  override def assetDistribution(assetId: IssuedAsset): AssetDistribution = {
    val fromInner = baseProvider.assetDistribution(assetId)
    val fromNg    = AssetDistribution(changedBalances(_.assets.getOrElse(assetId, 0L) != 0, blockchain.balance(_, assetId)))
    Monoid.combine(fromInner, fromNg)
  }

  override def assetDistributionAtHeight(assetId: IssuedAsset,
                                         height: Int,
                                         count: Int,
                                         fromAddress: Option[Address]): Either[ValidationError, AssetDistributionPage] = {
    baseProvider.assetDistributionAtHeight(assetId, height, count, fromAddress)
  }

  override def plixDistribution(height: Int): Either[ValidationError, Map[Address, Long]] = {
    getDiff().fold(baseProvider.plixDistribution(height)) { _ =>
      val innerDistribution = baseProvider.plixDistribution(height)
      if (height < blockchain.height) innerDistribution
      else {
        innerDistribution.map(_ ++ changedBalances(_.balance != 0, blockchain.balance(_)))
      }
    }
  }

  private def changedBalances(pred: Portfolio => Boolean, f: Address => Long): Map[Address, Long] = {
    getDiff()
      .fold(Map.empty[Address, Long]) { diff =>
        for {
          (address, p) <- diff.portfolios
          if pred(p)
        } yield address -> f(address)
      }
  }
}
