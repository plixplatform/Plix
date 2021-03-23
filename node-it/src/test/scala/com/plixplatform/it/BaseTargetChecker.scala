package com.plixlatform.it

import com.typesafe.config.ConfigFactory.{defaultApplication, defaultReference}
import com.plixlatform.account.PublicKey
import com.plixlatform.block.Block
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils.EitherExt2
import com.plixlatform.consensus.PoSSelector
import com.plixlatform.db.openDB
import com.plixlatform.history.StorageFactory
import com.plixlatform.settings._
import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.utils.NTP
import monix.execution.UncaughtExceptionReporter
import monix.reactive.Observer
import net.ceedubs.ficus.Ficus._

object BaseTargetChecker {
  def main(args: Array[String]): Unit = {
    val sharedConfig = Docker.genesisOverride
      .withFallback(Docker.configTemplate)
      .withFallback(defaultApplication())
      .withFallback(defaultReference())
      .resolve()

    val settings          = PlixSettings.fromRootConfig(sharedConfig)
    val db                = openDB("/tmp/tmp-db")
    val ntpTime           = new NTP("ntp.pool.org")
    val portfolioChanges  = Observer.empty(UncaughtExceptionReporter.default)
    val blockchainUpdater = StorageFactory(settings, db, ntpTime, portfolioChanges)
    val poSSelector       = new PoSSelector(blockchainUpdater, settings.blockchainSettings, settings.synchronizationSettings)

    try {
      val genesisBlock = Block.genesis(settings.blockchainSettings.genesisSettings).explicitGet()
      blockchainUpdater.processBlock(genesisBlock)

      NodeConfigs.Default.map(_.withFallback(sharedConfig)).collect {
        case cfg if cfg.as[Boolean]("plix.miner.enable") =>
          val account   = PublicKey(cfg.as[ByteStr]("public-key").arr)
          val address   = account.toAddress
          val balance   = blockchainUpdater.balance(address, Plix)
          val consensus = genesisBlock.consensusData
          val timeDelay = poSSelector
            .getValidBlockDelay(blockchainUpdater.height, account, consensus.baseTarget, balance)
            .explicitGet()

          f"$address: ${timeDelay * 1e-3}%10.3f s"
      }
    } finally ntpTime.close()
  }
}
