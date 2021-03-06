package com.plixplatform.api.common
import com.plixplatform.state.{AssetDescription, Blockchain}
import com.plixplatform.transaction.Asset.IssuedAsset
import com.plixplatform.transaction.assets.IssueTransaction

class CommonAssetsApi(blockchain: Blockchain) {
  import CommonAssetsApi._

  def description(assetId: IssuedAsset): Option[AssetDescription] = {
    blockchain.assetDescription(assetId)
  }

  def fullInfo(assetId: IssuedAsset): Option[AssetInfo] =
    for {
      assetInfo                               <- blockchain.assetDescription(assetId)
      (_, issueTransaction: IssueTransaction) <- blockchain.transactionInfo(assetId.id)
      sponsorBalance = if (assetInfo.sponsorship != 0) Some(blockchain.plixPortfolio(issueTransaction.sender).spendableBalance) else None
    } yield AssetInfo(assetInfo, issueTransaction, sponsorBalance)
}

object CommonAssetsApi {
  final case class AssetInfo(description: AssetDescription, issueTransaction: IssueTransaction, sponsorBalance: Option[Long])
}
