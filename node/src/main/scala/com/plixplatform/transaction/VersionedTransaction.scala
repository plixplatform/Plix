package com.plixplatform.transaction

trait VersionedTransaction {
  def version: Byte
}
