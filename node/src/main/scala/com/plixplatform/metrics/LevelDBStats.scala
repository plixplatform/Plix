package com.plixplatform.metrics

import com.plixplatform.database.Key
import kamon.Kamon
import kamon.metric.{HistogramMetric, MeasurementUnit}

object LevelDBStats {
  implicit class DbHistogramExt(val h: HistogramMetric) {
    def recordTagged(key: Key[_], value: Array[Byte]): Unit = recordTagged(key.name, value)

    def recordTagged(tag: String, value: Array[Byte]): Unit =
      h.refine("key", tag).record(Option(value).map(_.length.toLong).getOrElse(0))

    def recordTagged(tag: String, totalBytes: Long): Unit =
      h.refine("key", tag).record(totalBytes)
  }

  val miss: HistogramMetric  = Kamon.histogram("node.db.cachemiss")
  val read: HistogramMetric  = Kamon.histogram("node.db.read", MeasurementUnit.information.bytes)
  val write: HistogramMetric = Kamon.histogram("node.db.write", MeasurementUnit.information.bytes)
}
