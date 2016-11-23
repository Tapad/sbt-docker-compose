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

class BasicAppSpec extends fixture.FunSuite with fixture.ConfigMapFixture with Eventually with IntegrationPatience with Matchers {

  // The configMap passed to each test case will contain the connection information for the running Docker Compose
  // services. The key into the map is "serviceName:containerPort" and it will return "host:hostPort" which is the
  // Docker Compose generated endpoint that can be connected to at runtime. You can use this to endpoint connect to
  // for testing. Each service will also inject a "serviceName:containerId" key with the value equal to the container id.
  // You can use this to emulate service failures by killing and restarting the container.
  val basicServiceName = "basic"
  val basicServiceHostKey = s"$basicServiceName:8080"
  val basicServiceContainerIdKey = s"$basicServiceName:containerId"

  test("Validate that the Docker Compose endpoint returns a success code and the string 'Hello, World!'", DockerComposeTag) {
    configMap =>{
      println(configMap)
      val hostInfo = getHostInfo(configMap)
      val containerId = getContainerId(configMap)

      println(s"Attempting to connect to: $hostInfo, container id is $containerId")

      eventually {
        val output = Http(s"http://$hostInfo").asString
        output.isSuccess shouldBe true
        output.body should include ("Hello, World!")
      }
    }
  }

  test("Example Untagged Test. Will not be run.") {
    configMap =>
  }

  test("Validate presence of docker config information in system properties", DockerComposeTag) {
    configMap =>
      Option(System.getProperty(basicServiceHostKey)) shouldBe defined
  }

  def getHostInfo(configMap: ConfigMap): String = getContainerSetting(configMap, basicServiceHostKey)
  def getContainerId(configMap: ConfigMap): String = getContainerSetting(configMap, basicServiceContainerIdKey)

  def getContainerSetting(configMap: ConfigMap, key: String): String = {
    if (configMap.keySet.contains(key)) {
      configMap(key).toString
    }
    else {
      throw new TestFailedException(s"Cannot find the expected Docker Compose service key '$key' in the configMap", 10)
    }
  }
}
