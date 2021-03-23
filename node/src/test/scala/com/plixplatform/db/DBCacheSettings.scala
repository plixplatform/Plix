package com.plixplatform.db
import com.typesafe.config.ConfigFactory
import com.plixplatform.settings.PlixSettings

trait DBCacheSettings {
  lazy val dbSettings = PlixSettings.fromRootConfig(ConfigFactory.load()).dbSettings
  lazy val maxCacheSize: Int = dbSettings.maxCacheSize
}
