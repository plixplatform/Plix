package com.plixplatform.lang.v1.traits.domain
import com.plixplatform.common.state.ByteStr

case class BlockInfo(timestamp: Long,
                     height: Int,
                     baseTarget: Long,
                     generationSignature: ByteStr,
                     generator: ByteStr,
                     generatorPublicKey: ByteStr)
