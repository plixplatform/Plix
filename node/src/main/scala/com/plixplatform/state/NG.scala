package com.plixplatform.state

import com.plixplatform.block.Block.BlockId
import com.plixplatform.block.MicroBlock
import com.plixplatform.common.state.ByteStr

trait NG extends Blockchain {
  def microBlock(id: ByteStr): Option[MicroBlock]

  def bestLastBlockInfo(maxTimestamp: Long): Option[BlockMinerInfo]

  def lastPersistedBlockIds(count: Int): Seq[BlockId]

  def microblockIds: Seq[BlockId]
}
