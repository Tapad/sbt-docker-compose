import com.tapad.docker.{ PortInfo, RunningInstanceInfo, ServiceInfo, DockerComposePluginLocal }
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }

class PrintFormattingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest {
  test("Validate table printing succeeds when no Ports are exposed") {
    val plugin = new DockerComposePluginLocal
    val service = new ServiceInfo("service", "image", "source", List.empty)
    val instance = new RunningInstanceInfo("instance", "service", "composePath", List(service))
    plugin.printMappedPortInformation(instance)
  }

  test("Validate table printing succeeds when Ports are exposed") {
    val plugin = new DockerComposePluginLocal
    val port = new PortInfo("host", "container", false)
    val service = new ServiceInfo("service", "image", "source", List(port))
    val instance = new RunningInstanceInfo("instance", "service", "composePath", List(service))
    plugin.printMappedPortInformation(instance)
  }
}
