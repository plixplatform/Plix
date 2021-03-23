package com.plixlatform.state

import com.plixlatform.block.Block.BlockId
import com.plixlatform.block.MicroBlock
import com.plixlatform.common.state.ByteStr

trait NG extends Blockchain {
  def microBlock(id: ByteStr): Option[MicroBlock]

  def bestLastBlockInfo(maxTimestamp: Long): Option[BlockMinerInfo]

  def lastPersistedBlockIds(count: Int): Seq[BlockId]

  def microblockIds: Seq[BlockId]
}
