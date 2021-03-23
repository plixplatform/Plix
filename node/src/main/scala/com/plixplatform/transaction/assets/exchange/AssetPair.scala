package com.plixlatform.transaction.assets.exchange

import com.plixlatform.common.state.ByteStr
import com.plixlatform.serialization.Deser
import com.plixlatform.transaction.Asset.{IssuedAsset, Plix}
import com.plixlatform.transaction._
import com.plixlatform.transaction.assets.exchange.Order.assetIdBytes
import com.plixlatform.transaction.assets.exchange.Validation.booleanOperators
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import net.ceedubs.ficus.readers.ValueReader
import play.api.libs.json.{JsObject, Json}

import scala.annotation.meta.field
import scala.util.{Success, Try}

@ApiModel
case class AssetPair(@(ApiModelProperty @field)(
                       value = "Base58 encoded amount asset id",
                       dataType = "string",
                       example = "PLIX"
                     ) amountAsset: Asset,
                     @(ApiModelProperty @field)(
                       value = "Base58 encoded amount price id",
                       dataType = "string",
                       example = "8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS"
                     ) priceAsset: Asset) {
  import AssetPair._

  @ApiModelProperty(hidden = true)
  lazy val priceAssetStr: String = assetIdStr(priceAsset)
  @ApiModelProperty(hidden = true)
  lazy val amountAssetStr: String = assetIdStr(amountAsset)
  override def toString: String   = key
  def key: String                 = amountAssetStr + "-" + priceAssetStr
  def isValid: Validation         = (amountAsset != priceAsset) :| "Invalid AssetPair"
  def bytes: Array[Byte]          = assetIdBytes(amountAsset) ++ assetIdBytes(priceAsset)
  def json: JsObject = Json.obj(
    "amountAsset" -> amountAsset.maybeBase58Repr,
    "priceAsset"  -> priceAsset.maybeBase58Repr
  )
  def reverse = AssetPair(priceAsset, amountAsset)

  def assets: Set[Asset] = Set(amountAsset, priceAsset)
}

object AssetPair {
  val WavesName = "WAVES" // TODO : during the transition
  val PlixName = "PLIX"

  def assetIdStr(aid: Asset): String = aid match {
    case Plix           => PlixName
    case IssuedAsset(id) => id.base58
  }

  def extractAssetId(a: String): Try[Asset] = a match {
    case `WavesName` => Success(Plix) // TODO : during the transition
    case `PlixName` => Success(Plix)
    case other       => ByteStr.decodeBase58(other).map(IssuedAsset)
  }

  def createAssetPair(amountAsset: String, priceAsset: String): Try[AssetPair] =
    for {
      a1 <- extractAssetId(amountAsset)
      a2 <- extractAssetId(priceAsset)
    } yield AssetPair(a1, a2)

  def fromBytes(xs: Array[Byte]): AssetPair = {
    val (amount, offset) = Deser.parseByteArrayOption(xs, 0, AssetIdLength)
    val (price, _)       = Deser.parseByteArrayOption(xs, offset, AssetIdLength)
    AssetPair(
      Asset.fromCompatId(amount.map(ByteStr(_))),
      Asset.fromCompatId(price.map(ByteStr(_)))
    )
  }

  implicit val assetPairReader: ValueReader[AssetPair] = { (cfg, path) =>
    val source    = cfg.getString(path)
    val sourceArr = source.split("-")
    val res = sourceArr match {
      case Array(amtAssetStr, prcAssetStr) => AssetPair.createAssetPair(amtAssetStr, prcAssetStr)
      case _                               => throw new Exception(s"$source (incorrect assets count, expected 2 but got ${sourceArr.size})")
    }
    res fold (ex => throw new Exception(s"$source (${ex.getMessage})"), identity)
  }
}
