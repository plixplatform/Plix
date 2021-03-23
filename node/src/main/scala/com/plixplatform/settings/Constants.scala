package com.plixlatform.settings

import com.plixlatform.Version
import com.plixlatform.transaction.TransactionParsers
import com.plixlatform.utils.ScorexLogging

/**
  * System constants here.
  */
object Constants extends ScorexLogging {
  val ApplicationName = "plix"
  val AgentName       = s"Plix v${Version.VersionString}"

  val UnitsInWave = 100000000L
  val TotalPlix  = 100000000L

  lazy val TransactionNames: Map[Byte, String] =
    TransactionParsers.all.map {
      case ((typeId, _), builder) =>
        val txName =
          builder.getClass.getSimpleName.init
            .replace("V1", "")
            .replace("V2", "")

        typeId -> txName
    }
}
