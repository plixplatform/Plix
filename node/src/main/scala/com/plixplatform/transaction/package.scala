package com.plixlatform

import com.plixlatform.block.{Block, MicroBlock}
import com.plixlatform.utils.base58Length

package object transaction {
  val AssetIdLength: Int       = com.plixlatform.crypto.DigestSize
  val AssetIdStringLength: Int = base58Length(AssetIdLength)
  type DiscardedTransactions = Seq[Transaction]
  type DiscardedBlocks       = Seq[Block]
  type DiscardedMicroBlocks  = Seq[MicroBlock]
  type AuthorizedTransaction = Authorized with Transaction
}
