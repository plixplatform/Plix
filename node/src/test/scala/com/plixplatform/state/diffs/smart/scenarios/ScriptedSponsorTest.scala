package com.plixlatform.state.diffs.smart.scenarios

import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.features.BlockchainFeatures
import com.plixlatform.lagonaki.mocks.TestBlock
import com.plixlatform.settings.TestFunctionalitySettings
import com.plixlatform.transaction.Asset.{IssuedAsset, Plix}
import com.plixlatform.transaction.assets.{IssueTransactionV1, SponsorFeeTransaction}
import com.plixlatform.transaction.smart.SetScriptTransaction
import com.plixlatform.transaction.smart.script.ScriptCompiler
import com.plixlatform.transaction.transfer.{TransferTransaction, TransferTransactionV2}
import com.plixlatform.transaction.{GenesisTransaction, Transaction}
import com.plixlatform.{NoShrink, TransactionGen}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class ScriptedSponsorTest extends PropSpec with PropertyChecks with Matchers with TransactionGen with NoShrink {

  import com.plixlatform.state.diffs._

  val ENOUGH_FEE: Long = 100000000

  val fs = TestFunctionalitySettings.Enabled
    .copy(
      preActivatedFeatures = Map(
        BlockchainFeatures.NG.id                              -> 0,
        BlockchainFeatures.MassTransfer.id                    -> 0,
        BlockchainFeatures.SmartAccounts.id                   -> 0,
        BlockchainFeatures.DataTransaction.id                 -> 0,
        BlockchainFeatures.BurnAnyTokens.id                   -> 0,
        BlockchainFeatures.FeeSponsorship.id                  -> 0,
        BlockchainFeatures.FairPoS.id                         -> 0,
        BlockchainFeatures.SmartAssets.id                     -> 0,
        BlockchainFeatures.SmartAccountTrading.id             -> 0,
        BlockchainFeatures.SmallerMinimalGeneratingBalance.id -> 0
      ),
      featureCheckBlocksPeriod = 1,
      blocksForFeatureActivation = 1
    )

  property("sponsorship works when used by scripted accounts") {
    forAll(separateContractAndSponsor) {
      case (setupTxs, transfer) =>
        val setupBlocks   = setupTxs.map(TestBlock.create)
        val transferBlock = TestBlock.create(Seq(transfer))

        val IssuedAsset(assetId) = transfer.feeAssetId
        val contract             = transfer.sender

        val contractSpent: Long = ENOUGH_FEE + 1
        val sponsorSpent: Long  = ENOUGH_FEE * 3 - 1 + ENOUGH_FEE * FeeValidation.FeeUnit

        val sponsor = setupTxs.flatten.collectFirst { case t: SponsorFeeTransaction => t.sender }.get

        assertDiffAndState(setupBlocks :+ TestBlock.create(Nil), transferBlock, fs) { (diff, blck) =>
          blck.balance(contract, IssuedAsset(assetId)) shouldEqual ENOUGH_FEE * 2
          blck.balance(contract) shouldEqual ENOUGH_AMT - contractSpent

          blck.balance(sponsor, IssuedAsset(assetId)) shouldEqual Long.MaxValue - ENOUGH_FEE * 2
          blck.balance(sponsor) shouldEqual ENOUGH_AMT - sponsorSpent
        }
    }
  }

  property("sponsorship works when sponsored by scripted accounts") {
    forAll(scriptedSponsor) {
      case (setupTxs, transfer) =>
        val setupBlocks   = setupTxs.map(TestBlock.create)
        val transferBlock = TestBlock.create(Seq(transfer))

        val IssuedAsset(assetId) = transfer.feeAssetId
        val contract             = setupTxs.flatten.collectFirst { case t: SponsorFeeTransaction => t.sender }.get
        val recipient            = transfer.sender

        val contractSpent: Long  = ENOUGH_FEE * 4 + ENOUGH_FEE * FeeValidation.FeeUnit
        val recipientSpent: Long = 1

        assertDiffAndState(setupBlocks :+ TestBlock.create(Nil), transferBlock, fs) { (diff, blck) =>
          blck.balance(contract, IssuedAsset(assetId)) shouldEqual Long.MaxValue - ENOUGH_FEE * 2
          blck.balance(contract) shouldEqual ENOUGH_AMT - contractSpent

          blck.balance(recipient, IssuedAsset(assetId)) shouldEqual ENOUGH_FEE * 2
          blck.balance(recipient) shouldEqual ENOUGH_AMT - recipientSpent
        }
    }
  }

  val scriptedSponsor = {
    val timestamp = System.currentTimeMillis()
    for {
      contract  <- accountGen
      recipient <- accountGen
      gen1 = GenesisTransaction
        .create(contract, ENOUGH_AMT, timestamp)
        .explicitGet()
      gen2 = GenesisTransaction
        .create(recipient, ENOUGH_AMT, timestamp)
        .explicitGet()
      (script, _) = ScriptCompiler(s"false", isAssetScript = false).explicitGet()
      issueTx = IssueTransactionV1
        .selfSigned(
          sender = contract,
          name = "Asset#1".getBytes("UTF-8"),
          description = "description".getBytes("UTF-8"),
          quantity = Long.MaxValue,
          decimals = 8,
          reissuable = false,
          fee = ENOUGH_FEE,
          timestamp = timestamp + 2
        )
        .explicitGet()
      sponsorTx = SponsorFeeTransaction
        .selfSigned(
          contract,
          IssuedAsset(issueTx.id()),
          Some(1),
          ENOUGH_FEE,
          timestamp + 4
        )
        .explicitGet()
      transferToRecipient = TransferTransactionV2
        .selfSigned(
          IssuedAsset(issueTx.id()),
          contract,
          recipient,
          ENOUGH_FEE * 3,
          System.currentTimeMillis() + 4,
          Plix,
          ENOUGH_FEE,
          Array.emptyByteArray
        )
        .explicitGet()
      setScript = SetScriptTransaction
        .selfSigned(
          contract,
          Some(script),
          ENOUGH_FEE,
          System.currentTimeMillis() + 6
        )
        .explicitGet()
      transferTx = TransferTransactionV2
        .selfSigned(
          Plix,
          recipient,
          accountGen.sample.get,
          1,
          System.currentTimeMillis() + 8,
          IssuedAsset(issueTx.id()),
          ENOUGH_FEE,
          Array.emptyByteArray
        )
        .explicitGet()
    } yield (Seq(Seq(gen1, gen2), Seq(issueTx, sponsorTx), Seq(transferToRecipient, setScript)), transferTx)
  }

  val separateContractAndSponsor: Gen[(Seq[Seq[Transaction]], TransferTransaction)] = {
    val timestamp = System.currentTimeMillis()
    for {
      contract <- accountGen
      sponsor  <- accountGen
      gen1 = GenesisTransaction
        .create(contract, ENOUGH_AMT, timestamp)
        .explicitGet()
      gen2 = GenesisTransaction
        .create(sponsor, ENOUGH_AMT, timestamp)
        .explicitGet()
      (script, _) = ScriptCompiler(s"true", isAssetScript = false).explicitGet()
      issueTx = IssueTransactionV1
        .selfSigned(
          sender = sponsor,
          name = "Asset#1".getBytes("UTF-8"),
          description = "description".getBytes("UTF-8"),
          quantity = Long.MaxValue,
          decimals = 8,
          reissuable = false,
          fee = ENOUGH_FEE,
          timestamp = timestamp + 2
        )
        .explicitGet()
      sponsorTx = SponsorFeeTransaction
        .selfSigned(
          sponsor,
          IssuedAsset(issueTx.id()),
          Some(1),
          ENOUGH_FEE,
          timestamp + 4
        )
        .explicitGet()
      transferToContract = TransferTransactionV2
        .selfSigned(
          IssuedAsset(issueTx.id()),
          sponsor,
          contract,
          ENOUGH_FEE * 3,
          System.currentTimeMillis() + 4,
          Plix,
          ENOUGH_FEE,
          Array.emptyByteArray
        )
        .explicitGet()
      setScript = SetScriptTransaction
        .selfSigned(
          contract,
          Some(script),
          ENOUGH_FEE,
          System.currentTimeMillis() + 6
        )
        .explicitGet()
      transferTx = TransferTransactionV2
        .selfSigned(
          Plix,
          contract,
          sponsor,
          1,
          System.currentTimeMillis() + 8,
          IssuedAsset(issueTx.id()),
          ENOUGH_FEE,
          Array.emptyByteArray
        )
        .explicitGet()
    } yield (Seq(Seq(gen1, gen2), Seq(issueTx, sponsorTx), Seq(transferToContract, setScript)), transferTx)
  }
}
