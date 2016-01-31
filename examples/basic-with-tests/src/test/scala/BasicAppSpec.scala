import org.scalatest._
import scala.Console._
import scala.sys.process._
import scalaj.http.Http
import org.scalatest.Tag
import org.scalatest.concurrent._
import org.scalatest.exceptions._
import java.io.{ByteArrayOutputStream, PrintWriter}

//Use you define a specific tag to indicate which test should be run against the Docker Compose instance
object DockerComposeTag extends Tag("DockerComposeTag")

class BasicAppSpec extends fixture.FunSuite with fixture.ConfigMapFixture with Eventually with IntegrationPatience with ShouldMatchers {

  // The configMap passed to each test case will contain the connection information for the running Docker Compose
  // services. The key into the map is "serviceName:containerPort" and it will return "host:hostPort" which is the
  // Docker Compose generated endpoint that can be connected to at runtime. You can use this to endpoint connect to
  // for testing.
  val basicServiceKey = "basic:8080"

  test("Validate the Docker Compose endpoint is available and returns a successful Http code when queried", DockerComposeTag) {
    configMap =>{
      val hostInfo = getHostInfo(configMap)
      println(s"Attempting to connect to: $hostInfo")

      eventually {
        Http(s"http://$hostInfo").asString.isSuccess shouldBe true
      }
    }
  }

  test("Example Untagged Test. Will not be run.") {
    configMap =>
  }

  def getHostInfo(configMap: ConfigMap): String = {
    if (configMap.keySet.exists(_ == basicServiceKey)) {
      configMap(basicServiceKey).toString
    }
    else {
      throw new TestFailedException(s"Cannot find the expected Docker Compose service '$basicServiceKey' in the configMap", 10)
    }
  }
}