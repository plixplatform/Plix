package com.plixplatform.settings

import com.typesafe.config.{Config, ConfigFactory}
import com.plixplatform.metrics.Metrics
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration.FiniteDuration

case class PlixSettings(directory: String,
                         ntpServer: String,
                         dbSettings: DBSettings,
                         extensions: Seq[String],
                         extensionsShutdownTimeout: FiniteDuration,
                         networkSettings: NetworkSettings,
                         walletSettings: WalletSettings,
                         blockchainSettings: BlockchainSettings,
                         minerSettings: MinerSettings,
                         restAPISettings: RestAPISettings,
                         synchronizationSettings: SynchronizationSettings,
                         utxSettings: UtxSettings,
                         featuresSettings: FeaturesSettings,
                         metrics: Metrics.Settings,
                         nodeStatus: Boolean,
                         config: Config)

object PlixSettings extends CustomValueReaders {
  def fromRootConfig(rootConfig: Config): PlixSettings = {
    val plix = rootConfig.getConfig("plix")

    val directory                 = plix.as[String]("directory")
    val ntpServer                 = plix.as[String]("ntp-server")
    val dbSettings                = plix.as[DBSettings]("db")
    val extensions                = plix.as[Seq[String]]("extensions")
    val extensionsShutdownTimeout = plix.as[FiniteDuration]("extensions-shutdown-timeout")
    val networkSettings           = plix.as[NetworkSettings]("network")
    val walletSettings            = plix.as[WalletSettings]("wallet")
    val blockchainSettings        = plix.as[BlockchainSettings]("blockchain")
    val minerSettings             = plix.as[MinerSettings]("miner")
    val restAPISettings           = plix.as[RestAPISettings]("rest-api")
    val synchronizationSettings   = plix.as[SynchronizationSettings]("synchronization")
    val utxSettings               = plix.as[UtxSettings]("utx")
    val featuresSettings          = plix.as[FeaturesSettings]("features")
    val metrics                   = plix.as[Metrics.Settings]("metrics")
    val nodeStatus                = plix.as[Boolean]("node-status")

    PlixSettings(
      directory,
      ntpServer,
      dbSettings,
      extensions,
      extensionsShutdownTimeout,
      networkSettings,
      walletSettings,
      blockchainSettings,
      minerSettings,
      restAPISettings,
      synchronizationSettings,
      utxSettings,
      featuresSettings,
      metrics,
      nodeStatus,
      rootConfig
    )
  }

  def default(): PlixSettings = fromRootConfig(ConfigFactory.load())
}
