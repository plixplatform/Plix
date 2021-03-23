package com.plixplatform.transaction.smart

import java.io.BufferedWriter
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit

import com.plixplatform.account.KeyPair
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils._
import com.plixplatform.lang.v1.compiler.Terms
import com.plixplatform.lang.v1.compiler.Terms.{CONST_BOOLEAN, EVALUATED}
import com.plixplatform.lang.v1.evaluator.Log
import com.plixplatform.lang.v1.evaluator.ctx.impl.plix.Bindings
import com.plixplatform.state.BinaryDataEntry
import com.plixplatform.transaction.DataTransaction
import com.plixplatform.transaction.smart.VerifierLoggerBenchmark.BigLog
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
@Threads(1)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
class VerifierLoggerBenchmark {

  @Benchmark
  def verifierLogged(bh: Blackhole, log: BigLog): Unit = {
    val logs = Verifier.buildLogs("id", log.value)
    bh.consume(log.writer.write(logs))
  }
}

object VerifierLoggerBenchmark {

  @State(Scope.Benchmark)
  class BigLog {

    val resultFile: Path       = Paths.get("log.txt")
    val writer: BufferedWriter = Files.newBufferedWriter(resultFile)

    private val dataTx: DataTransaction = DataTransaction.selfSigned(
      KeyPair(Array[Byte]()),
      (1 to 4).map(i => BinaryDataEntry(s"data$i", ByteStr(Array.fill(1024 * 30)(1)))).toList,
      100000000,
      0
    ).explicitGet()

    private val dataTxObj: Terms.CaseObj = Bindings.transactionObject(
      RealTransactionWrapper(dataTx),
      proofsEnabled = true
    )

    val value: (Log, Either[String, EVALUATED]) =
      (
        List.fill(500)("txVal" -> Right(dataTxObj)),
        Right(CONST_BOOLEAN(true))
      )

    @TearDown
    def deleteFile(): Unit = {
      Files.delete(resultFile)
      writer.close()
    }
  }
}
