package com.plixlatform.history

import com.plixlatform.TransactionGen
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.features.BlockchainFeatures
import com.plixlatform.settings.{BlockchainSettings, PlixSettings}
import com.plixlatform.state.diffs.{ENOUGH_AMT, produce}
import com.plixlatform.transaction.Asset.IssuedAsset
import com.plixlatform.transaction.assets.{BurnTransactionV1, IssueTransactionV1, ReissueTransactionV1}
import com.plixlatform.transaction.transfer.TransferTransactionV1
import com.plixlatform.transaction.{Asset, GenesisTransaction}
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class BlockchainUpdaterBurnTest extends PropSpec with PropertyChecks with DomainScenarioDrivenPropertyCheck with Matchers with TransactionGen {

  val Plix: Long = 100000000

  type Setup =
    (Long, GenesisTransaction, TransferTransactionV1, IssueTransactionV1, BurnTransactionV1, ReissueTransactionV1)

  val preconditions: Gen[Setup] = for {
    master                                                   <- accountGen
    ts                                                       <- timestampGen
    transferAssetPlixFee                                    <- smallFeeGen
    alice                                                    <- accountGen
    (_, assetName, description, quantity, decimals, _, _, _) <- issueParamGen
    genesis: GenesisTransaction = GenesisTransaction.create(master, ENOUGH_AMT, ts).explicitGet()
    masterToAlice: TransferTransactionV1 = TransferTransactionV1
      .selfSigned(Asset.Plix, master, alice, 3 * Plix, ts + 1, Asset.Plix, transferAssetPlixFee, Array.emptyByteArray)
      .explicitGet()
    issue: IssueTransactionV1 = IssueTransactionV1
      .selfSigned(alice, assetName, description, quantity, decimals, reissuable = false, Plix, ts + 100)
      .explicitGet()
    burn: BurnTransactionV1 = BurnTransactionV1.selfSigned(alice, IssuedAsset(issue.assetId), quantity / 2, Plix, ts + 200).explicitGet()
    reissue: ReissueTransactionV1 = ReissueTransactionV1
      .selfSigned(alice, IssuedAsset(issue.assetId), burn.quantity, reissuable = true, Plix, ts + 300)
      .explicitGet()
  } yield (ts, genesis, masterToAlice, issue, burn, reissue)

  val localBlockchainSettings: BlockchainSettings = DefaultBlockchainSettings.copy(
    functionalitySettings = DefaultBlockchainSettings.functionalitySettings
      .copy(
        featureCheckBlocksPeriod = 1,
        blocksForFeatureActivation = 1,
        preActivatedFeatures = Map(BlockchainFeatures.NG.id -> 0, BlockchainFeatures.DataTransaction.id -> 0)
      ))
  val localPlixSettings: PlixSettings = settings.copy(blockchainSettings = localBlockchainSettings)

  property("issue -> burn -> reissue in sequential blocks works correctly") {
    scenario(preconditions, localPlixSettings) {
      case (domain, (ts, genesis, masterToAlice, issue, burn, reissue)) =>
        val block0 = customBuildBlockOfTxs(randomSig, Seq(genesis), defaultSigner, 1, ts)
        val block1 = customBuildBlockOfTxs(block0.uniqueId, Seq(masterToAlice), defaultSigner, 1, ts + 150)
        val block2 = customBuildBlockOfTxs(block1.uniqueId, Seq(issue), defaultSigner, 1, ts + 250)
        val block3 = customBuildBlockOfTxs(block2.uniqueId, Seq(burn), defaultSigner, 1, ts + 350)
        val block4 = customBuildBlockOfTxs(block3.uniqueId, Seq(reissue), defaultSigner, 1, ts + 450)

        domain.appendBlock(block0)
        domain.appendBlock(block1)

        domain.appendBlock(block2)
        val assetDescription1 = domain.blockchainUpdater.assetDescription(IssuedAsset(issue.assetId)).get
        assetDescription1.reissuable should be(false)
        assetDescription1.totalVolume should be(issue.quantity)

        domain.appendBlock(block3)
        val assetDescription2 = domain.blockchainUpdater.assetDescription(IssuedAsset(issue.assetId)).get
        assetDescription2.reissuable should be(false)
        assetDescription2.totalVolume should be(issue.quantity - burn.quantity)

        domain.blockchainUpdater.processBlock(block4) should produce("Asset is not reissuable")
    }
  }

  property("issue -> burn -> reissue in micro blocks works correctly") {
    scenario(preconditions, localPlixSettings) {
      case (domain, (ts, genesis, masterToAlice, issue, burn, reissue)) =>
        val block0 = customBuildBlockOfTxs(randomSig, Seq(genesis), defaultSigner, 1, ts)
        val block1 = customBuildBlockOfTxs(block0.uniqueId, Seq(masterToAlice), defaultSigner, 1, ts + 150)
        val block2 = customBuildBlockOfTxs(block1.uniqueId, Seq(issue), defaultSigner, 1, ts + 250)
        val block3 = customBuildBlockOfTxs(block2.uniqueId, Seq(burn, reissue), defaultSigner, 1, ts + 350)

        domain.appendBlock(block0)
        domain.appendBlock(block1)

        domain.appendBlock(block2)
        val assetDescription1 = domain.blockchainUpdater.assetDescription(IssuedAsset(issue.assetId)).get
        assetDescription1.reissuable should be(false)
        assetDescription1.totalVolume should be(issue.quantity)

        domain.blockchainUpdater.processBlock(block3) should produce("Asset is not reissuable")
    }
  }
}
