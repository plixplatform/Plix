package com.plixplatform.protobuf

import com.plixplatform
import com.plixplatform.transaction.assets.exchange.Order

package object transaction {
  type PBOrder = com.plixplatform.protobuf.transaction.ExchangeTransactionData.Order
  val PBOrder: ExchangeTransactionData.Order.type = com.plixplatform.protobuf.transaction.ExchangeTransactionData.Order

  type VanillaOrder = com.plixplatform.transaction.assets.exchange.Order
  val VanillaOrder: Order.type = com.plixplatform.transaction.assets.exchange.Order

  type PBTransaction = com.plixplatform.protobuf.transaction.Transaction
  val PBTransaction: Transaction.type = com.plixplatform.protobuf.transaction.Transaction

  type PBSignedTransaction = com.plixplatform.protobuf.transaction.SignedTransaction
  val PBSignedTransaction: SignedTransaction.type = com.plixplatform.protobuf.transaction.SignedTransaction

  type VanillaTransaction = com.plixplatform.transaction.Transaction
  val VanillaTransaction: plixplatform.transaction.Transaction.type = com.plixplatform.transaction.Transaction

  type VanillaSignedTransaction = com.plixplatform.transaction.SignedTransaction

  type VanillaAssetId = com.plixplatform.transaction.Asset
}
