package com.plixplatform.lang.v1.traits.domain

import com.plixplatform.common.state.ByteStr

case class Ord(id: ByteStr,
               sender: Recipient.Address,
               senderPublicKey: ByteStr,
               matcherPublicKey: ByteStr,
               assetPair: APair,
               orderType: OrdType,
               price: Long,
               amount: Long,
               timestamp: Long,
               expiration: Long,
               matcherFee: Long,
               bodyBytes: ByteStr,
               proofs: IndexedSeq[ByteStr],
               matcherFeeAssetId: Option[ByteStr] = None)
