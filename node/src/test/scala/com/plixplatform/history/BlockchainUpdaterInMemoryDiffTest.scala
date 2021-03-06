package com.plixplatform.history

import com.plixplatform.TransactionGen
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.features.BlockchainFeatures
import com.plixplatform.state.diffs._
import com.plixplatform.transaction._
import com.plixplatform.transaction.transfer._
import org.scalacheck.Gen
import org.scalatest._
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class BlockchainUpdaterInMemoryDiffTest
    extends PropSpec
    with PropertyChecks
    with DomainScenarioDrivenPropertyCheck
    with Matchers
    with TransactionGen {
  val preconditionsAndPayments: Gen[(GenesisTransaction, TransferTransactionV1, TransferTransactionV1)] = for {
    master    <- accountGen
    recipient <- accountGen
    ts        <- positiveIntGen
    genesis: GenesisTransaction = GenesisTransaction.create(master, ENOUGH_AMT, ts).explicitGet()
    payment: TransferTransactionV1  <- plixTransferGeneratorP(master, recipient)
    payment2: TransferTransactionV1 <- plixTransferGeneratorP(master, recipient)
  } yield (genesis, payment, payment2)

  property("compaction with liquid block doesn't make liquid block affect state once") {
    assume(BlockchainFeatures.implemented.contains(BlockchainFeatures.SmartAccounts.id))
    scenario(preconditionsAndPayments) {
      case (domain, (genesis, payment1, payment2)) =>
        val blocksWithoutCompaction = chainBlocks(
          Seq(genesis) +:
            Seq.fill(MaxTransactionsPerBlockDiff * 2 - 1)(Seq.empty[Transaction]) :+
            Seq(payment1))
        val blockTriggersCompaction = buildBlockOfTxs(blocksWithoutCompaction.last.uniqueId, Seq(payment2))

        blocksWithoutCompaction.foreach(b => domain.blockchainUpdater.processBlock(b).explicitGet())
        val mastersBalanceAfterPayment1 = domain.portfolio(genesis.recipient).balance
        mastersBalanceAfterPayment1 shouldBe (ENOUGH_AMT - payment1.amount - payment1.fee)

        domain.blockchainUpdater.height shouldBe MaxTransactionsPerBlockDiff * 2 + 1

        domain.blockchainUpdater.processBlock(blockTriggersCompaction).explicitGet()

        domain.blockchainUpdater.height shouldBe MaxTransactionsPerBlockDiff * 2 + 2

        val mastersBalanceAfterPayment1AndPayment2 = domain.blockchainUpdater.balance(genesis.recipient)
        mastersBalanceAfterPayment1AndPayment2 shouldBe (ENOUGH_AMT - payment1.amount - payment1.fee - payment2.amount - payment2.fee)
    }
  }
  property("compaction without liquid block doesn't make liquid block affect state once") {
    assume(BlockchainFeatures.implemented.contains(BlockchainFeatures.SmartAccounts.id))
    scenario(preconditionsAndPayments) {
      case (domain, (genesis, payment1, payment2)) =>
        val firstBlocks             = chainBlocks(Seq(Seq(genesis)) ++ Seq.fill(MaxTransactionsPerBlockDiff * 2 - 2)(Seq.empty[Transaction]))
        val payment1Block           = buildBlockOfTxs(firstBlocks.last.uniqueId, Seq(payment1))
        val emptyBlock              = buildBlockOfTxs(payment1Block.uniqueId, Seq.empty)
        val blockTriggersCompaction = buildBlockOfTxs(payment1Block.uniqueId, Seq(payment2))

        firstBlocks.foreach(b => domain.blockchainUpdater.processBlock(b).explicitGet())
        domain.blockchainUpdater.processBlock(payment1Block).explicitGet()
        domain.blockchainUpdater.processBlock(emptyBlock).explicitGet()
        val mastersBalanceAfterPayment1 = domain.blockchainUpdater.balance(genesis.recipient)
        mastersBalanceAfterPayment1 shouldBe (ENOUGH_AMT - payment1.amount - payment1.fee)

        // discard liquid block
        domain.blockchainUpdater.removeAfter(payment1Block.uniqueId)
        domain.blockchainUpdater.processBlock(blockTriggersCompaction).explicitGet()

        domain.blockchainUpdater.height shouldBe MaxTransactionsPerBlockDiff * 2 + 1

        val mastersBalanceAfterPayment1AndPayment2 = domain.blockchainUpdater.balance(genesis.recipient)
        mastersBalanceAfterPayment1AndPayment2 shouldBe (ENOUGH_AMT - payment1.amount - payment1.fee - payment2.amount - payment2.fee)
    }
  }
}
