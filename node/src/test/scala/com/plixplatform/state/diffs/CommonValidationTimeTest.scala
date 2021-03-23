package com.plixlatform.state.diffs

import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.db.WithState
import com.plixlatform.settings.TestFunctionalitySettings.Enabled
import com.plixlatform.state._
import com.plixlatform.{NoShrink, TransactionGen}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class CommonValidationTimeTest extends PropSpec with PropertyChecks with Matchers with TransactionGen with NoShrink with WithState {

  property("disallows too old transacions") {
    forAll(for {
      prevBlockTs <- timestampGen
      blockTs     <- timestampGen
      master      <- accountGen
      height      <- positiveIntGen
      recipient   <- accountGen
      amount      <- positiveLongGen
      fee         <- smallFeeGen
      transfer1 = createPlixTransfer(master, recipient, amount, fee, prevBlockTs - Enabled.maxTransactionTimeBackOffset.toMillis - 1)
        .explicitGet()
    } yield (prevBlockTs, blockTs, height, transfer1)) {
      case (prevBlockTs, blockTs, height, transfer1) =>
        withStateAndHistory(Enabled) { blockchain: Blockchain =>
          val result = TransactionDiffer(Some(prevBlockTs), blockTs, height)(blockchain, transfer1).resultE
          result should produce("in the past relative to previous block timestamp")
        }
    }
  }

  property("disallows transactions from far future") {
    forAll(for {
      prevBlockTs <- timestampGen
      blockTs     <- Gen.choose(prevBlockTs, prevBlockTs + 7 * 24 * 3600 * 1000)
      master      <- accountGen
      height      <- positiveIntGen
      recipient   <- accountGen
      amount      <- positiveLongGen
      fee         <- smallFeeGen
      transfer1 = createPlixTransfer(master, recipient, amount, fee, blockTs + Enabled.maxTransactionTimeForwardOffset.toMillis + 1)
        .explicitGet()
    } yield (prevBlockTs, blockTs, height, transfer1)) {
      case (prevBlockTs, blockTs, height, transfer1) =>
        val functionalitySettings = Enabled.copy(allowTransactionsFromFutureUntil = blockTs - 1)
        withStateAndHistory(functionalitySettings) { blockchain: Blockchain =>
          TransactionDiffer(Some(prevBlockTs), blockTs, height)(blockchain, transfer1).resultE should
            produce("in the future relative to block timestamp")
        }
    }
  }
}
