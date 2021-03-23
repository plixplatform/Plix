package com.plixlatform.features.api

import com.plixlatform.features.BlockchainFeatureStatus

case class FeatureActivationStatus(id: Short,
                                   description: String,
                                   blockchainStatus: BlockchainFeatureStatus,
                                   nodeStatus: NodeFeatureStatus,
                                   activationHeight: Option[Int],
                                   supportingBlocks: Option[Int])
