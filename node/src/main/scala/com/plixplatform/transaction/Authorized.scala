package com.plixplatform.transaction
import com.plixplatform.account.PublicKey

trait Authorized {
  val sender: PublicKey
}
