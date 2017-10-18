package example

import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers
import org.scalatest.concurrent.{Eventually, ScalaFutures}

object ServiceSteps {
  lazy val defaultStartedService = {
    CalculatorServer.start(8080)
  }
}

class ServiceSteps extends ScalaDsl with EN with Matchers with ScalaFutures with Eventually {

  var lastResult = Int.MinValue
  var client: CalculatorClient = null

  /**
    * This assumes a running service mapped against the host machine  at the given location
    */
  Given("""^a calculator client against (.+)$""") { hostPort: String =>
    client = CalculatorClient(hostPort)

    // prove connectivity eagerly within this step
    client.add(0, 0) shouldBe 0
  }

  Given("""^a remote request to add (.+) and (.+)$""") { (lhs: Int, rhs: Int) =>
    lastResult = client.add(lhs, rhs)
  }
  Given("""^a remote request to subtract (.+) from (.+)$""") { (rhs: Int, lhs: Int) =>
    lastResult = client.subtract(lhs, rhs)
  }
  Then("""^The response should be ([-0-9]+)$""") { (expected: Int) =>
    lastResult shouldBe expected
  }
}
