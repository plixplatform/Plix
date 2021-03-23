package com.plixlatform.extensions

import akka.actor.ActorSystem
import com.plixlatform.account.Address
import com.plixlatform.settings.PlixSettings
import com.plixlatform.state.Blockchain
import com.plixlatform.transaction.{Asset, Transaction}
import com.plixlatform.utils.Time
import com.plixlatform.utx.UtxPool
import com.plixlatform.wallet.Wallet
import monix.reactive.Observable

trait Context {
  def settings: PlixSettings
  def blockchain: Blockchain
  def time: Time
  def wallet: Wallet
  def utx: UtxPool
  def broadcastTx(tx: Transaction): Unit
  def spendableBalanceChanged: Observable[(Address, Asset)]
  def actorSystem: ActorSystem
}
