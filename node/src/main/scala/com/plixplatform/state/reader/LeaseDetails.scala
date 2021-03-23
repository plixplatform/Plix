package com.plixplatform.state.reader

import com.plixplatform.account.{PublicKey, AddressOrAlias}

case class LeaseDetails(sender: PublicKey, recipient: AddressOrAlias, height: Int, amount: Long, isActive: Boolean)
