package com.plixplatform.http

import com.plixplatform.api.http.ApiError.ApiKeyNotValid
import com.plixplatform.settings.PlixSettings
import com.plixplatform.{NTPTime, TestWallet}

//noinspection ScalaStyle
class DebugApiRouteSpec extends RouteSpec("/debug") with RestAPISettingsHelper with TestWallet with NTPTime {

  private val sampleConfig  = com.typesafe.config.ConfigFactory.load()
  private val plixSettings = PlixSettings.fromRootConfig(sampleConfig)
  private val configObject  = sampleConfig.root()
  private val route =
    DebugApiRoute(plixSettings, ntpTime, null, null, null, null, null, null, null, null, null, null, null, null, null, configObject).route

  routePath("/configInfo") - {
    "requires api-key header" in {
      Get(routePath("/configInfo?full=true")) ~> route should produce(ApiKeyNotValid)
      Get(routePath("/configInfo?full=false")) ~> route should produce(ApiKeyNotValid)
    }
  }
}
