package com.plixlatform.transaction

import com.google.common.primitives.{Bytes, Longs}
import com.plixlatform.account.Alias
import com.plixlatform.serialization.Deser
import com.plixlatform.transaction.Asset.Plix
import monix.eval.Coeval
import play.api.libs.json.{JsObject, Json}

trait CreateAliasTransaction extends ProvenTransaction with VersionedTransaction {
  def alias: Alias
  def fee: Long
  def timestamp: Long

  override val assetFee: (Asset, Long) = (Plix, fee)

  override val json: Coeval[JsObject] = Coeval.evalOnce(
    jsonBase() ++ Json.obj(
      "version"   -> version,
      "alias"     -> alias.name,
      "fee"       -> fee,
      "timestamp" -> timestamp
    ))

  val baseBytes: Coeval[Array[Byte]] = Coeval.evalOnce {
    Bytes.concat(
      sender,
      Deser.serializeArray(alias.bytes.arr),
      Longs.toByteArray(fee),
      Longs.toByteArray(timestamp)
    )
  }
}

object CreateAliasTransaction {
  val typeId: Byte = 10
}
