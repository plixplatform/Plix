package com.plixplatform

import com.plixplatform.block.{Block, MicroBlock}
import com.plixplatform.utils.base58Length

package object transaction {
  val AssetIdLength: Int       = com.plixplatform.crypto.DigestSize
  val AssetIdStringLength: Int = base58Length(AssetIdLength)
  type DiscardedTransactions = Seq[Transaction]
  type DiscardedBlocks       = Seq[Block]
  type DiscardedMicroBlocks  = Seq[MicroBlock]
  type AuthorizedTransaction = Authorized with Transaction
}
