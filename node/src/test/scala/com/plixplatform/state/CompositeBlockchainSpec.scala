package com.plixplatform.state

import com.plixplatform.state.reader.CompositeBlockchain
import com.plixplatform.utils.EmptyBlockchain
import com.plixplatform.{BlockGen, NoShrink}
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class CompositeBlockchainSpec extends FreeSpec with Matchers with PropertyChecks with BlockGen with NoShrink {
  "blockHeaderAndSize at current height is last block" in forAll(randomSignerBlockGen) { block =>
    val comp = CompositeBlockchain(EmptyBlockchain, newBlock = Some(block))

    comp.blockHeaderAndSize(comp.height).map(_._1.signerData.signature) should contain(block.uniqueId)
  }
}
