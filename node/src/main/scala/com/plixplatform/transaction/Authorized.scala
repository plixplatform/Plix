package com.plixlatform.transaction
import com.plixlatform.account.PublicKey

trait Authorized {
  val sender: PublicKey
}
