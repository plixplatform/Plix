package com.plixlatform.lang.v1.traits.domain

import com.plixlatform.common.state.ByteStr

case class APair(amountAsset: Option[ByteStr], priceAsset: Option[ByteStr])
