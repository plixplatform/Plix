package com.plixlatform.state

import com.plixlatform.block.Block.BlockId
import com.plixlatform.consensus.nxt.NxtLikeConsensusBlockData

case class BlockMinerInfo(consensus: NxtLikeConsensusBlockData, timestamp: Long, blockId: BlockId)
