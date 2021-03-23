package com.plixplatform.lang.v1.traits.domain

import com.plixplatform.common.state.ByteStr

case class APair(amountAsset: Option[ByteStr], priceAsset: Option[ByteStr])
