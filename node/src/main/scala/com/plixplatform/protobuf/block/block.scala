package com.plixlatform.protobuf

import com.plixlatform

package object block {
  type PBBlock = com.plixlatform.protobuf.block.Block
  val PBBlock: Block.type = com.plixlatform.protobuf.block.Block

  type VanillaBlock = com.plixlatform.block.Block
  val VanillaBlock: plixlatform.block.Block.type = com.plixlatform.block.Block
}
