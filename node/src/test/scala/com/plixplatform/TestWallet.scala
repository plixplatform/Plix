package com.plixplatform

import com.plixplatform.settings.WalletSettings
import com.plixplatform.wallet.Wallet

trait TestWallet {
  protected val testWallet: Wallet = {
    val wallet = Wallet(WalletSettings(None, Some("123"), None))
    wallet.generateNewAccounts(10)
    wallet
  }
}
