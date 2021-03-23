package com.plixplatform.lang.v1.traits.domain

import com.plixplatform.common.state.ByteStr

trait Recipient
object Recipient {
  case class Address(bytes: ByteStr) extends Recipient
  case class Alias(name: String)     extends Recipient
}
