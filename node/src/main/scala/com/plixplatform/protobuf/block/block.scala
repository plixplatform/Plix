package com.plixplatform.protobuf

import com.plixplatform

package object block {
  type PBBlock = com.plixplatform.protobuf.block.Block
  val PBBlock: Block.type = com.plixplatform.protobuf.block.Block

  type VanillaBlock = com.plixplatform.block.Block
  val VanillaBlock: plixplatform.block.Block.type = com.plixplatform.block.Block
}
