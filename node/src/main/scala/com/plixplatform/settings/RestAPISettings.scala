package com.plixplatform.settings

case class RestAPISettings(enable: Boolean,
                           bindAddress: String,
                           port: Int,
                           apiKeyHash: String,
                           cors: Boolean,
                           https: Boolean,
                           httpsPassword: String,
                           certificateFile: String,
                           apiKeyDifferentHost: Boolean,
                           transactionsByAddressLimit: Int,
                           distributionAddressLimit: Int,
                           allowTxRebroadcasting: Boolean,
                           maxBlocksPerRequest: Int)
