package com.plixlatform.state.extensions

import cats.kernel.Monoid
import com.plixlatform.account.Address
import com.plixlatform.lang.ValidationError
import com.plixlatform.state.{AssetDistribution, AssetDistributionPage, Portfolio}
import com.plixlatform.transaction.Asset.IssuedAsset
import com.plixlatform.transaction.assets.IssueTransaction
import com.plixlatform.utils.Paged
import monix.reactive.Observable

trait Distributions {
  def portfolio(a: Address): Portfolio

  def assetDistribution(asset: IssuedAsset): AssetDistribution
  def assetDistributionAtHeight(asset: IssuedAsset,
                                height: Int,
                                count: Int,
                                fromAddress: Option[Address]): Either[ValidationError, AssetDistributionPage]

  def plixDistribution(height: Int): Either[ValidationError, Map[Address, Long]]

  def nftObservable(address: Address, from: Option[IssuedAsset]): Observable[IssueTransaction]
}

object Distributions {
  def apply[T](value: T)(implicit ev: T => Distributions): Distributions = value

  trait Prov[T] {
    def distributions(value: T): Distributions
  }

  case object Empty extends Distributions {
    override def portfolio(a: Address): Portfolio = Portfolio.empty

    override def assetDistribution(asset: IssuedAsset): AssetDistribution = AssetDistribution @@ Map.empty[Address, Long]

    override def assetDistributionAtHeight(asset: IssuedAsset,
                                           height: Int,
                                           count: Int,
                                           fromAddress: Option[Address]): Either[ValidationError, AssetDistributionPage] =
      Right(AssetDistributionPage(Paged[Address, AssetDistribution](hasNext = false, None, Monoid.empty[AssetDistribution])))

    override def plixDistribution(height: Int): Either[ValidationError, Map[Address, Long]] = Right(Map.empty)

    override def nftObservable(address: Address, from: Option[IssuedAsset]): Observable[IssueTransaction] = Observable.empty
  }
}
