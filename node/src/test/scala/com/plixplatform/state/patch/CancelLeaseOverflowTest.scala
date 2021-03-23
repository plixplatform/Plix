package com.plixplatform.state.patch

import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.lagonaki.mocks.TestBlock
import com.plixplatform.settings.TestFunctionalitySettings
import com.plixplatform.state.diffs._
import com.plixplatform.transaction.Asset.Plix
import com.plixplatform.transaction.GenesisTransaction
import com.plixplatform.transaction.lease.LeaseTransactionV1
import com.plixplatform.transaction.transfer._
import com.plixplatform.{NoShrink, TransactionGen}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class CancelLeaseOverflowTest extends PropSpec with PropertyChecks with Matchers with TransactionGen with NoShrink {

  private val settings = TestFunctionalitySettings.Enabled.copy(blockVersion3AfterHeight = 5)

  property("CancelLeaseOverflow cancels active outgoing leases for accounts having negative spendable balances") {
    val leaseOverflowGen = for {
      sender1   <- accountGen
      sender2   <- accountGen
      recipient <- accountGen
      amount    <- positiveLongGen
      fee       <- smallFeeGen
      ts        <- timestampGen
    } yield
      (
        GenesisTransaction.create(sender1, amount + fee, ts).explicitGet(),
        GenesisTransaction.create(sender2, amount + fee * 2, ts).explicitGet(),
        LeaseTransactionV1.selfSigned(sender1, amount, fee, ts, sender2).explicitGet(),
        LeaseTransactionV1.selfSigned(sender2, amount, fee, ts, recipient).explicitGet(),
        TransferTransactionV1.selfSigned(Plix, sender2, recipient, amount, ts, Plix, fee, Array.emptyByteArray).explicitGet()
      )

    forAll(leaseOverflowGen) {
      case (gt1, gt2, lease1, lease2, tx) =>
        assertDiffAndState(
          Seq(TestBlock.create(Seq(gt1, gt2)), TestBlock.create(Seq(lease1)), TestBlock.create(Seq(lease2, tx)), TestBlock.create(Seq.empty)),
          TestBlock.create(Seq.empty),
          settings
        ) {
          case (_, newState) =>
            newState.leaseDetails(lease2.id()).forall(_.isActive) shouldBe false
            newState.leaseDetails(lease1.id()).exists(_.isActive) shouldBe true
        }
    }
  }
}
