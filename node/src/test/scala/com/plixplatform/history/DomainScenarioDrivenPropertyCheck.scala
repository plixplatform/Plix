package com.plixlatform.history

import com.plixlatform.db.WithDomain
import com.plixlatform.settings.PlixSettings
import org.scalacheck.Gen
import org.scalatest.{Assertion, Suite}
import org.scalatestplus.scalacheck.{ScalaCheckDrivenPropertyChecks => GeneratorDrivenPropertyChecks}

trait DomainScenarioDrivenPropertyCheck extends WithDomain { _: Suite with GeneratorDrivenPropertyChecks =>
  def scenario[S](gen: Gen[S], bs: PlixSettings = DefaultPlixSettings)(assertion: (Domain, S) => Assertion): Assertion =
    forAll(gen) { s =>
      withDomain(bs) { domain =>
        assertion(domain, s)
      }
    }
}
