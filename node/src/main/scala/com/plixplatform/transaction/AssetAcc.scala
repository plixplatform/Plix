package com.plixlatform.transaction

import com.plixlatform.account.Address

case class AssetAcc(account: Address, assetId: Option[Asset])
