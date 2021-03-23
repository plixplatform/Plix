package com.plixplatform.http

import com.typesafe.config.ConfigFactory
import com.plixplatform.common.utils.Base58
import com.plixplatform.crypto
import com.plixplatform.settings._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

trait RestAPISettingsHelper {
  private val apiKey: String = "test_api_key"

  val ApiKeyHeader = api_key(apiKey)

  lazy val MaxTransactionsPerRequest = 10000
  lazy val MaxAddressesPerRequest    = 10000

  lazy val restAPISettings = {
    val keyHash = Base58.encode(crypto.secureHash(apiKey.getBytes("UTF-8")))
    ConfigFactory
      .parseString(
        s"""plix.rest-api {
           |  api-key-hash = $keyHash
           |  transactions-by-address-limit = $MaxTransactionsPerRequest
           |  distribution-by-address-limit = $MaxAddressesPerRequest
           |}
         """.stripMargin
      )
      .withFallback(ConfigFactory.load())
      .as[RestAPISettings]("plix.rest-api")
  }
}
