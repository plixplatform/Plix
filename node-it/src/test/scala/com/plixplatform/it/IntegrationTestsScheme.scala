package com.plixplatform.it

import com.plixplatform.account.AddressScheme

trait IntegrationTestsScheme {
  AddressScheme.current = new AddressScheme {
    override val chainId: Byte = 'I'
  }
}
