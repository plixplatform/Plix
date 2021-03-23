package com.plixplatform.it

import java.net.{InetSocketAddress, URL}

import com.typesafe.config.Config
import com.plixplatform.account.{KeyPair, PublicKey}
import com.plixplatform.common.utils.{Base58, EitherExt2}
import com.plixplatform.it.util.GlobalTimer
import com.plixplatform.settings.PlixSettings
import com.plixplatform.state.diffs.FeeValidation
import com.plixplatform.utils.LoggerFacade
import org.asynchttpclient.Dsl.{config => clientConfig, _}
import org.asynchttpclient._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

abstract class Node(val config: Config) extends AutoCloseable {
  lazy val log: LoggerFacade =
    LoggerFacade(LoggerFactory.getLogger(s"${getClass.getCanonicalName}.${this.name}"))

  val settings: PlixSettings = PlixSettings.fromRootConfig(config)
  val client: AsyncHttpClient = asyncHttpClient(
    clientConfig()
      .setKeepAlive(false)
      .setNettyTimer(GlobalTimer.instance))

  val privateKey: KeyPair  = KeyPair.fromSeed(config.getString("account-seed")).explicitGet()
  val publicKey: PublicKey = PublicKey.fromBase58String(config.getString("public-key")).explicitGet()
  val address: String      = config.getString("address")

  def nodeExternalPort(internalPort: Int): Int
  def nodeApiEndpoint: URL
  def apiKey: String

  /** An address which can be reached from the host running IT (may not match the declared address) */
  def networkAddress: InetSocketAddress

  override def close(): Unit = client.close()
}

object Node {
  implicit class NodeExt(val n: Node) extends AnyVal {
    def name: String               = n.settings.networkSettings.nodeName
    def publicKeyStr: String       = Base58.encode(n.publicKey)
    def fee(txTypeId: Byte): Long  = FeeValidation.OldFeeUnits(txTypeId) * FeeValidation.FeeUnit
    def blockDelay: FiniteDuration = n.settings.blockchainSettings.genesisSettings.averageBlockDelay
  }
}
