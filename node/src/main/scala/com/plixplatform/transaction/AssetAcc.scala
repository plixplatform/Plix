package com.plixplatform.transaction

import com.plixplatform.account.Address

case class AssetAcc(account: Address, assetId: Option[Asset])
