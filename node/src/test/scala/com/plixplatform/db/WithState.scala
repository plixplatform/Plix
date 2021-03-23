package com.plixlatform.db

import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import com.plixlatform.account.Address
import com.plixlatform.database.LevelDBWriter
import com.plixlatform.history.Domain
import com.plixlatform.settings.{PlixSettings, FunctionalitySettings, loadConfig}
import com.plixlatform.state.BlockchainUpdaterImpl
import com.plixlatform.transaction.Asset
import com.plixlatform.utils.Implicits.SubjectOps
import com.plixlatform.{NTPTime, TestHelpers}
import monix.reactive.Observer
import monix.reactive.subjects.Subject
import org.scalatest.Suite

trait WithState extends DBCacheSettings {
  protected val ignoreSpendableBalanceChanged: Subject[(Address, Asset), (Address, Asset)] = Subject.empty
  protected def withState[A](fs: FunctionalitySettings)(f: LevelDBWriter => A): A = {
    val path = Files.createTempDirectory("leveldb-test")
    val db   = openDB(path.toAbsolutePath.toString)
    try f(new LevelDBWriter(db, ignoreSpendableBalanceChanged, fs, dbSettings))
    finally {
      db.close()
      TestHelpers.deleteRecursively(path)
    }
  }

  def withStateAndHistory(fs: FunctionalitySettings)(test: LevelDBWriter => Any): Unit = withState(fs)(test)

  def withLevelDBWriter[A](fs: FunctionalitySettings)(test: LevelDBWriter => A): A = {
    val path = Files.createTempDirectory("leveldb-test")
    val db   = openDB(path.toAbsolutePath.toString)
    try test(new LevelDBWriter(db, Observer.stopped, fs, dbSettings))
    finally {
      db.close()
      TestHelpers.deleteRecursively(path)
    }
  }
}

trait WithDomain extends WithState with NTPTime {
  _: Suite =>

  def withDomain[A](settings: PlixSettings = PlixSettings.fromRootConfig(loadConfig(ConfigFactory.load())))(test: Domain => A): A = {
    try withState(settings.blockchainSettings.functionalitySettings) { blockchain =>
      val bcu = new BlockchainUpdaterImpl(blockchain, ignoreSpendableBalanceChanged, settings, ntpTime)
      try test(Domain(bcu, blockchain))
      finally bcu.shutdown()
    } finally {}
  }
}
