package com.plixplatform.account

abstract class AddressScheme {
  val chainId: Byte
  override def toString: String = s"AddressScheme($chainId)"
}

object AddressScheme {
  @volatile var current: AddressScheme = DefaultAddressScheme
}

object DefaultAddressScheme extends AddressScheme {
  val chainId: Byte = 'K'.toByte
}
