package example

import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers


class CalculatorSteps extends ScalaDsl with EN with Matchers {

  var lastResult = Int.MinValue

  Given("""^(.+) \+ (.+)$""") { (lhs: Int, rhs: Int) =>
    lastResult = Calculator.add(lhs, rhs)
  }
  Given("""^(.+) - (.+)$""") { (lhs: Int, rhs: Int) =>
    lastResult = Calculator.subtract(lhs, rhs)
  }
  Then("""^The result should be ([-0-9]+)$""") { (expected: Int) =>
    lastResult shouldBe expected
  }
}
