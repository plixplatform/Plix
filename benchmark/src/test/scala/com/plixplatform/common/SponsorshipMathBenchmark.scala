package com.plixlatform.common
import java.util.concurrent.TimeUnit

import com.plixlatform.state.diffs.FeeValidation
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
@Threads(4)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
class SponsorshipMathBenchmark {
  @Benchmark
  def bigDecimal_test(bh: Blackhole): Unit = {
    def toPlix(assetFee: Long, sponsorship: Long): Long = {
      val plix = (BigDecimal(assetFee) * BigDecimal(FeeValidation.FeeUnit)) / BigDecimal(sponsorship)
      if (plix > Long.MaxValue) {
        throw new java.lang.ArithmeticException("Overflow")
      }
      plix.toLong
    }

    bh.consume(toPlix(100000, 100000000))
  }

  @Benchmark
  def bigInt_test(bh: Blackhole): Unit = {
    def toPlix(assetFee: Long, sponsorship: Long): Long = {
      val plix = BigInt(assetFee) * FeeValidation.FeeUnit / sponsorship
      plix.bigInteger.longValueExact()
    }

    bh.consume(toPlix(100000, 100000000))
  }
}
