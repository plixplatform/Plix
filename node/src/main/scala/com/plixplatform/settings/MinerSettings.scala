package com.plixlatform.settings

import com.plixlatform.mining.Miner

import scala.concurrent.duration.FiniteDuration

case class MinerSettings(enable: Boolean,
                         quorum: Int,
                         intervalAfterLastBlockThenGenerationIsAllowed: FiniteDuration,
                         noQuorumMiningDelay: FiniteDuration,
                         microBlockInterval: FiniteDuration,
                         minimalBlockGenerationOffset: FiniteDuration,
                         maxTransactionsInKeyBlock: Int,
                         maxTransactionsInMicroBlock: Int,
                         minMicroBlockAge: FiniteDuration,
                         maxPackTime: FiniteDuration) {
  require(maxTransactionsInMicroBlock <= Miner.MaxTransactionsPerMicroblock)
}
