package com.plixplatform.it.sync.network

import com.typesafe.config.{Config, ConfigFactory}
import com.plixplatform.it.api.SyncHttpApi._
import com.plixplatform.it.{DockerBased, NodeConfigs}
import com.plixplatform.utils.ScorexLogging
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent._
import scala.concurrent.duration._

class NetworkUniqueConnectionsTestSuite extends FreeSpec with Matchers with DockerBased with ScorexLogging {
  import NetworkUniqueConnectionsTestSuite._

  "nodes should up and connect with each other" in {
    val firstNode = docker.startNode(FirstNodeConfig)

    val status = firstNode.status()

    log.trace(s"#### $status")

    assert(status.blockchainHeight >= status.stateHeight)

    val secondNode = {
      // Helps to do an incoming connection: second -> first (1)
      val peersConfig = ConfigFactory.parseString(
        s"""plix.network.known-peers = [
             |  "${firstNode.containerNetworkAddress.getHostName}:${firstNode.containerNetworkAddress.getPort}"
             |]""".stripMargin
      )

      docker.startNode(peersConfig.withFallback(SecondNodeConfig), autoConnect = false)
    }
    firstNode.waitForPeers(1)

    // Outgoing connection: first -> second (2)
    firstNode.connect(secondNode.containerNetworkAddress)

    withClue("Should fail with TimeoutException, because the connectionAttempt should fail") {
      intercept[TimeoutException] { firstNode.waitForPeers(2, 30.seconds) }
    }
  }

}

object NetworkUniqueConnectionsTestSuite {

  private val configs          = NodeConfigs.newBuilder.withDefault(0).withSpecial(2, _.nonMiner).build()
  val FirstNodeConfig: Config  = configs.head
  val SecondNodeConfig: Config = configs.last

}
