package com.plixplatform.protobuf.utils
import com.google.protobuf.ByteString
import com.plixplatform.account.PublicKey
import com.plixplatform.common.state.ByteStr
import com.plixplatform.lang.ValidationError
import com.plixplatform.protobuf.transaction._
import com.plixplatform.transaction.Asset
import com.plixplatform.transaction.Asset.{IssuedAsset, Plix}

private[protobuf] object PBInternalImplicits {
  import com.google.protobuf.{ByteString => PBByteString}
  import com.plixplatform.account.{AddressOrAlias, Address => VAddress, Alias => VAlias}

  implicit def byteStringToByteStr(bs: PBByteString): ByteStr = bs.toByteArray
  implicit def byteStrToByteString(bs: ByteStr): PBByteString = PBByteString.copyFrom(bs)

  implicit def fromAddressOrAlias(addressOrAlias: AddressOrAlias): Recipient = PBRecipients.create(addressOrAlias)

  implicit class PBRecipientImplicitConversionOps(recipient: Recipient) {
    def toAddress: Either[ValidationError, VAddress]              = PBRecipients.toAddress(recipient)
    def toAlias: Either[ValidationError, VAlias]                  = PBRecipients.toAlias(recipient)
    def toAddressOrAlias: Either[ValidationError, AddressOrAlias] = PBRecipients.toAddressOrAlias(recipient)
  }

  implicit def fromAssetIdAndAmount(v: (VanillaAssetId, Long)): Amount = v match {
    case (IssuedAsset(assetId), amount) =>
      Amount()
        .withAssetId(AssetId().withIssuedAsset(assetId))
        .withAmount(amount)

    case (Plix, amount) =>
      Amount()
        .withAssetId(AssetId().withPlix(com.google.protobuf.empty.Empty()))
        .withAmount(amount)
  }

  implicit class AmountImplicitConversions(a: Amount) {
    def longAmount: Long = a.amount
    def vanillaAssetId: Asset = PBAmounts.toVanillaAssetId(a.getAssetId)
  }

  implicit class PBByteStringOps(bs: PBByteString) {
    def byteStr: ByteStr            = ByteStr(bs.toByteArray)
    def publicKeyAccount: PublicKey = PublicKey(bs.toByteArray)
  }

  implicit def byteStringToByte(bytes: ByteString): Byte =
    if (bytes.isEmpty) 0
    else bytes.byteAt(0)

  implicit def byteToByteString(chainId: Byte): ByteString = {
    if (chainId == 0) ByteString.EMPTY else ByteString.copyFrom(Array(chainId))
  }
}
