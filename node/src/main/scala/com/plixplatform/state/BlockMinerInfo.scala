package com.plixplatform.state

import com.plixplatform.block.Block.BlockId
import com.plixplatform.consensus.nxt.NxtLikeConsensusBlockData

case class BlockMinerInfo(consensus: NxtLikeConsensusBlockData, timestamp: Long, blockId: BlockId)
