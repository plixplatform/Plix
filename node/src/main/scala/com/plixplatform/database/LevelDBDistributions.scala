package com.plixplatform.database

import cats.effect.Resource
import com.plixplatform.account.Address
import com.plixplatform.features.BlockchainFeatures
import com.plixplatform.lang.ValidationError
import com.plixplatform.state.extensions.Distributions
import com.plixplatform.state.{AddressId, AssetDistribution, AssetDistributionPage, Portfolio}
import com.plixplatform.transaction.Asset.IssuedAsset
import com.plixplatform.transaction.TxValidationError.GenericError
import com.plixplatform.transaction.assets.IssueTransaction
import com.plixplatform.utils.Paged
import monix.eval.Task
import monix.reactive.Observable

private[database] final class LevelDBDistributions(ldb: LevelDBWriter) extends Distributions {
  import LevelDBWriter._
  import com.plixplatform.features.FeatureProvider.FeatureProviderExt
  import ldb._

  def portfolio(a: Address): Portfolio =
    portfolioCache.get(a, () => loadPortfolio(a))

  def nftObservable(address: Address, from: Option[IssuedAsset]): Observable[IssueTransaction] = {
    def openIterator() = readOnlyNoClose { (snapshot, db) =>
      def issueTxIterator = {
        val assetIds = db
          .get(Keys.addressId(address))
          .fold(Seq.empty[IssuedAsset]) { id =>
            val addressId = AddressId @@ id
            db.get(Keys.assetList(addressId))
          }

        assetIds.iterator
          .flatMap(ia => transactionInfo(ia.id).map(_._2))
          .collect {
            case itx: IssueTransaction if itx.isNFT => itx
          }
      }

      val result = from
        .flatMap(ia => transactionInfo(ia.id))
        .fold(issueTxIterator) {
          case (_, afterTx) =>
            issueTxIterator
              .dropWhile(_.id() != afterTx.id())
              .drop(1)
        }

      (result, snapshot)
    }

    val resource = Resource(Task {
      val (iter, snapshot) = openIterator()
      (iter, Task(snapshot.close()))
    })
    Observable.fromIterator(resource)
  }

  override def assetDistribution(asset: IssuedAsset): AssetDistribution = readOnly { db =>
    val dst = (for {
      seqNr     <- (1 to db.get(Keys.addressesForAssetSeqNr(asset))).par
      addressId <- db.get(Keys.addressesForAsset(asset, seqNr)).par
      actualHeight <- db
        .get(Keys.assetBalanceHistory(addressId, asset))
        .filterNot(_ > height)
        .headOption
      balance = db.get(Keys.assetBalance(addressId, asset)(actualHeight))
      if balance > 0
    } yield db.get(Keys.idToAddress(addressId)) -> balance).toMap.seq

    AssetDistribution(dst)
  }

  override def assetDistributionAtHeight(asset: IssuedAsset,
                                         height: Int,
                                         count: Int,
                                         fromAddress: Option[Address]): Either[ValidationError, AssetDistributionPage] = readOnly { db =>
    val canGetAfterHeight = db.get(Keys.safeRollbackHeight)

    lazy val maybeAddressId = fromAddress.flatMap(addr => db.get(Keys.addressId(addr)))

    def takeAfter(s: Seq[BigInt], a: Option[BigInt]): Seq[BigInt] = {
      a match {
        case None    => s
        case Some(v) => s.dropWhile(_ != v).drop(1)
      }
    }

    lazy val addressIds: Seq[BigInt] = {
      val all = for {
        seqNr <- 1 to db.get(Keys.addressesForAssetSeqNr(asset))
        addressId <- db
          .get(Keys.addressesForAsset(asset, seqNr))
      } yield addressId

      takeAfter(all, maybeAddressId)
    }

    lazy val distribution: Stream[(Address, Long)] =
      for {
        addressId <- addressIds.toStream
        history = db.get(Keys.assetBalanceHistory(addressId, asset))
        actualHeight <- history.filterNot(_ > height).headOption
        balance = db.get(Keys.assetBalance(addressId, asset)(actualHeight))
        if balance > 0
      } yield db.get(Keys.idToAddress(addressId)) -> balance

    lazy val page: AssetDistributionPage = {
      val dst = distribution.take(count + 1)

      val hasNext = dst.length > count
      val items   = if (hasNext) dst.init else dst
      val lastKey = items.lastOption.map(_._1)

      val result: Paged[Address, AssetDistribution] =
        Paged(hasNext, lastKey, AssetDistribution(items.toMap))

      AssetDistributionPage(result)
    }

    Either
      .cond(
        height > canGetAfterHeight,
        page,
        GenericError(s"Cannot get asset distribution at height less than ${canGetAfterHeight + 1}")
      )
  }

  override def plixDistribution(height: Int): Either[ValidationError, Map[Address, Long]] = readOnly { db =>
    val canGetAfterHeight = db.get(Keys.safeRollbackHeight)

    def createMap() =
      (for {
        seqNr     <- (1 to db.get(Keys.addressesForPlixSeqNr)).par
        addressId <- db.get(Keys.addressesForPlix(seqNr)).par
        history = db.get(Keys.plixBalanceHistory(addressId))
        actualHeight <- history.partition(_ > height)._2.headOption
        balance = db.get(Keys.plixBalance(addressId)(actualHeight))
        if balance > 0
      } yield db.get(Keys.idToAddress(addressId)) -> balance).toMap.seq

    Either.cond(
      height > canGetAfterHeight,
      createMap(),
      GenericError(s"Cannot get plix distribution at height less than ${canGetAfterHeight + 1}")
    )
  }

  private[this] def loadFullPortfolio(db: ReadOnlyDB, addressId: BigInt) = loadLposPortfolio(db, addressId).copy(
    assets = (for {
      asset <- db.get(Keys.assetList(addressId))
    } yield asset -> db.fromHistory(Keys.assetBalanceHistory(addressId, asset), Keys.assetBalance(addressId, asset)).getOrElse(0L)).toMap
  )

  private def loadPortfolioWithoutNFT(db: ReadOnlyDB, addressId: AddressId) = loadLposPortfolio(db, addressId).copy(
    assets = (for {
      issuedAsset <- db.get(Keys.assetList(addressId))
      asset <- transactionInfo(issuedAsset.id).collect {
        case (_, it: IssueTransaction) if !it.isNFT => issuedAsset
      }
    } yield asset -> db.fromHistory(Keys.assetBalanceHistory(addressId, asset), Keys.assetBalance(addressId, asset)).getOrElse(0L)).toMap
  )

  private[this] def loadPortfolio(address: Address): Portfolio = readOnly { db =>
    val excludeNFT = ldb.isFeatureActivated(BlockchainFeatures.ReduceNFTFee, height)

    addressId(address).fold(Portfolio.empty) { addressId =>
      if (excludeNFT) loadPortfolioWithoutNFT(db, AddressId @@ addressId)
      else loadFullPortfolio(db, addressId)
    }
  }
}
