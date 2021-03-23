package com.plixplatform.transaction

import com.google.protobuf.ByteString
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.Base58
import com.plixplatform.protobuf.transaction.AssetId
import com.plixplatform.transaction.assets.exchange.AssetPair
import net.ceedubs.ficus.readers.ValueReader
import play.api.libs.json._

import scala.util.Success

sealed trait Asset
object Asset {
  final case class IssuedAsset(id: ByteStr) extends Asset
  case object Plix                         extends Asset

  implicit val assetReads: Reads[IssuedAsset] = Reads {
    case JsString(str) if str.length > AssetIdStringLength => JsError("invalid.feeAssetId")
    case JsString(str) =>
      Base58.tryDecodeWithLimit(str) match {
        case Success(arr) => JsSuccess(IssuedAsset(ByteStr(arr)))
        case _            => JsError("Expected base58-encoded assetId")
      }
    case _ => JsError("Expected base58-encoded assetId")
  }
  implicit val assetWrites: Writes[IssuedAsset] = Writes { asset =>
    JsString(asset.id.base58)
  }

  implicit val assetIdReads: Reads[Asset] = Reads {
    case json: JsString => assetReads.reads(json)
    case JsNull         => JsSuccess(Plix)
    case _              => JsError("Expected base58-encoded assetId or null")
  }
  implicit val assetIdWrites: Writes[Asset] = Writes {
    case Plix           => JsNull
    case IssuedAsset(id) => JsString(id.base58)
  }

  implicit val assetJsonFormat: Format[IssuedAsset] = Format(assetReads, assetWrites)
  implicit val assetIdJsonFormat: Format[Asset]     = Format(assetIdReads, assetIdWrites)

  implicit val assetReader: ValueReader[Asset] = { (cfg, path) =>
    AssetPair.extractAssetId(cfg getString path).fold(ex => throw new Exception(ex.getMessage), identity)
  }

  def fromString(maybeStr: Option[String]): Asset = {
    maybeStr.map(x => IssuedAsset(ByteStr.decodeBase58(x).get)).getOrElse(Plix)
  }

  def fromCompatId(maybeBStr: Option[ByteStr]): Asset = {
    maybeBStr.fold[Asset](Plix)(IssuedAsset)
  }

  def fromProtoId(byteStr: ByteString): Asset = {
    if (byteStr.isEmpty) Plix
    else IssuedAsset(byteStr.toByteArray)
  }

  def fromProtoId(assetId: AssetId): Asset = assetId.asset match {
    case AssetId.Asset.IssuedAsset(bs) => fromProtoId(bs)
    case _ => Plix
  }

  implicit class AssetIdOps(private val ai: Asset) extends AnyVal {
    def byteRepr: Array[Byte] = ai match {
      case Plix           => Array(0: Byte)
      case IssuedAsset(id) => (1: Byte) +: id.arr
    }

    def protoId: AssetId = ai match {
      case IssuedAsset(id) => AssetId().withIssuedAsset(ByteString.copyFrom(id))
      case Plix => AssetId().withPlix(com.google.protobuf.empty.Empty())
    }

    def compatId: Option[ByteStr] = ai match {
      case Plix           => None
      case IssuedAsset(id) => Some(id)
    }

    def maybeBase58Repr: Option[String] = ai match {
      case Plix           => None
      case IssuedAsset(id) => Some(id.base58)
    }

    def fold[A](onPlix: => A)(onAsset: IssuedAsset => A): A = ai match {
      case Plix                  => onPlix
      case asset @ IssuedAsset(_) => onAsset(asset)
    }
  }
}
