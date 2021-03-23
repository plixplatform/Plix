package com.plixlatform.it.sync.transactions

import com.plixlatform.it.api.SyncHttpApi._
import com.plixlatform.it.api.PaymentRequest
import com.plixlatform.it.transactions.BaseTransactionSuite
import com.plixlatform.it.util._
import org.scalatest.prop.TableDrivenPropertyChecks

class PaymentTransactionSuite extends BaseTransactionSuite with TableDrivenPropertyChecks {

  private val paymentAmount = 5.plix
  private val defaulFee     = 1.plix

  test("plix payment changes plix balances and eff.b.") {

    val (firstBalance, firstEffBalance)   = miner.accountBalances(firstAddress)
    val (secondBalance, secondEffBalance) = miner.accountBalances(secondAddress)

    val transferId = sender.payment(firstAddress, secondAddress, paymentAmount, defaulFee).id
    nodes.waitForHeightAriseAndTxPresent(transferId)
    miner.assertBalances(firstAddress, firstBalance - paymentAmount - defaulFee, firstEffBalance - paymentAmount - defaulFee)
    miner.assertBalances(secondAddress, secondBalance + paymentAmount, secondEffBalance + paymentAmount)
  }

  val payment = PaymentRequest(5.plix, 1.plix, firstAddress, secondAddress)
  val endpoints =
    Table("/plix/payment/signature", "/plix/create-signed-payment", "/plix/external-payment", "/plix/broadcast-signed-payment")
  forAll(endpoints) { (endpoint: String) =>
    test(s"obsolete endpoints respond with BadRequest. Endpoint:$endpoint") {
      val errorMessage = "This API is no longer supported"
      assertBadRequestAndMessage(sender.postJson(endpoint, payment), errorMessage)
    }
  }
}
