package com.plixlatform.transaction

trait VersionedTransaction {
  def version: Byte
}
