package com.plixlatform.db
import com.typesafe.config.ConfigFactory
import com.plixlatform.settings.PlixSettings

trait DBCacheSettings {
  lazy val dbSettings = PlixSettings.fromRootConfig(ConfigFactory.load()).dbSettings
  lazy val maxCacheSize: Int = dbSettings.maxCacheSize
}
