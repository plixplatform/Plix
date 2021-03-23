package com.plixplatform.extensions

import akka.actor.ActorSystem
import com.plixplatform.account.Address
import com.plixplatform.settings.PlixSettings
import com.plixplatform.state.Blockchain
import com.plixplatform.transaction.{Asset, Transaction}
import com.plixplatform.utils.Time
import com.plixplatform.utx.UtxPool
import com.plixplatform.wallet.Wallet
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
