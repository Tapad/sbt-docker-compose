import scala.Console._
import scala.sys.process._
import org.specs2._
import org.specs2.execute._
import scalaj.http.Http
import java.io.{ByteArrayOutputStream, PrintWriter}

class BasicAppSpec extends mutable.Specification  {

  // The System Properties will contain the connection information for the running Docker Compose
  // services. The key into the map is "serviceName:containerPort" and it will return "host:hostPort" which is the
  // Docker Compose generated endpoint that can be connected to at runtime. You can use this to endpoint connect to
  // for testing. Each service will also inject a "serviceName:containerId" key with the value equal to the container id.
  // You can use this to emulate service failures by killing and restarting the container.
  val basicServiceName = "basic"
  val basicServiceHostKey = s"$basicServiceName:8080"
  val basicServiceContainerIdKey = s"$basicServiceName:containerId"
  val hostInfo = getHostInfo()
  val containerId = getContainerId()

  "Validate that the Docker Compose endpoint returns a success code and the string 'Hello, World!'" >> {
      println(s"Attempting to connect to: $hostInfo, container id is $containerId")

      eventually {
        val output = Http(s"http://$hostInfo").asString
        output.isSuccess mustEqual true
        output.body must contain ("Hello, World!")
      }
  }

  def getHostInfo(): String = getContainerSetting(basicServiceHostKey)
  def getContainerId(): String = getContainerSetting(basicServiceContainerIdKey)

  def getContainerSetting(key: String): String = {
    if (System.getProperty(key) !=  null) {
      System.getProperty(key)
    }
    else {
      throw new FailureException(Failure(s"Cannot find the expected Docker Compose service key '$key' in the System Properties"))
    }
  }
}