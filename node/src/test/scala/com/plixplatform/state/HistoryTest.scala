package com.plixplatform.state

import com.plixplatform.block.Block
import com.plixplatform.common.state.ByteStr
import com.plixplatform.crypto._
import com.plixplatform.lagonaki.mocks.TestBlock

trait HistoryTest {
  val genesisBlock: Block = TestBlock.withReference(ByteStr(Array.fill(SignatureLength)(0: Byte)))

  def getNextTestBlock(blockchain: Blockchain): Block =
    TestBlock.withReference(blockchain.lastBlock.get.uniqueId)

  def getNextTestBlockWithVotes(blockchain: Blockchain, votes: Set[Short]): Block =
    TestBlock.withReferenceAndFeatures(blockchain.lastBlock.get.uniqueId, votes)
}
