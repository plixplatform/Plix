package com.plixlatform.protobuf

import com.plixlatform
import com.plixlatform.transaction.assets.exchange.Order

package object transaction {
  type PBOrder = com.plixlatform.protobuf.transaction.ExchangeTransactionData.Order
  val PBOrder: ExchangeTransactionData.Order.type = com.plixlatform.protobuf.transaction.ExchangeTransactionData.Order

  type VanillaOrder = com.plixlatform.transaction.assets.exchange.Order
  val VanillaOrder: Order.type = com.plixlatform.transaction.assets.exchange.Order

  type PBTransaction = com.plixlatform.protobuf.transaction.Transaction
  val PBTransaction: Transaction.type = com.plixlatform.protobuf.transaction.Transaction

  type PBSignedTransaction = com.plixlatform.protobuf.transaction.SignedTransaction
  val PBSignedTransaction: SignedTransaction.type = com.plixlatform.protobuf.transaction.SignedTransaction

  type VanillaTransaction = com.plixlatform.transaction.Transaction
  val VanillaTransaction: plixlatform.transaction.Transaction.type = com.plixlatform.transaction.Transaction

  type VanillaSignedTransaction = com.plixlatform.transaction.SignedTransaction

  type VanillaAssetId = com.plixlatform.transaction.Asset
}
