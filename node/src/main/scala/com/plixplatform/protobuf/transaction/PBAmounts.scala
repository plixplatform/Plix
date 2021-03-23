package com.plixplatform.protobuf.transaction
import com.google.protobuf.ByteString
import com.plixplatform.transaction.Asset
import com.plixplatform.transaction.Asset.{IssuedAsset, Plix}

object PBAmounts {
  def toPBAssetId(asset: Asset): AssetId = asset match {
    case Asset.IssuedAsset(id) =>
      AssetId().withIssuedAsset(ByteString.copyFrom(id))

    case Asset.Plix =>
      AssetId().withPlix(com.google.protobuf.empty.Empty())
  }

  def toVanillaAssetId(assetId: AssetId): Asset = assetId.asset match {
    case AssetId.Asset.Plix(_)             => Plix
    case AssetId.Asset.IssuedAsset(assetId) => IssuedAsset(assetId.toByteArray)
    case _ => throw new IllegalArgumentException
  }

  def fromAssetAndAmount(asset: Asset, amount: Long): Amount =
    Amount(Some(toPBAssetId(asset)), amount)

  def toAssetAndAmount(value: Amount): (Asset, Long) =
    (toVanillaAssetId(value.getAssetId), value.amount)
}
