import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker._
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }
import sbt.Keys._
import scala.io.Source

class PrintFormattingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest {
  test("Validate table printing succeeds when no Ports are exposed") {
    val plugin = new DockerComposePluginLocal
    val service = new ServiceInfo("service", "image", "source", List.empty)
    val instance = new RunningInstanceInfo("instance", "service", "composePath", List(service))
    plugin.printMappedPortInformation(null, instance, Version(1, 1, 11))
  }

  test("Validate table printing succeeds when Ports are exposed") {
    val plugin = new DockerComposePluginLocal
    val port = new PortInfo("host", "container", false)
    val service = new ServiceInfo("service", "image", "source", List(port))
    val instance = new RunningInstanceInfo("instance", "service", "composePath", List(service))
    plugin.printMappedPortInformation(null, instance, Version(1, 1, 11))
  }

  test("Validate the table output shows '<none>' when no Ports are exposed") {
    val (composeMock, composeFilePath) = getComposeMock("no_exposed_ports.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq.empty, composeYaml)
    doReturn("").when(composeMock).getDockerPortMappings("containerId01")
    val serviceInfoUpdated = composeMock.populateServiceInfoForInstance("123456", "default", serviceInfo, 1000)

    val tableOutput = composeMock.getTableOutputList(serviceInfoUpdated)

    assert(tableOutput.toList.exists(out => out.contains("<none>")))
  }

  test("Validate the table output displays protocol names for non-tcp protocols") {
    val (composeMock, composeFilePath) = getComposeMock("debug_port.yml")
    val dockerPortStream = getClass.getResourceAsStream("docker_port.txt")
    val portMappings = Source.fromInputStream(dockerPortStream).mkString
    doReturn(portMappings).when(composeMock).getDockerPortMappings("containerId01")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq.empty, composeYaml)
    val serviceInfoUpdated = composeMock.populateServiceInfoForInstance("123456", "default", serviceInfo, 1000)

    val tableOutput = composeMock.getTableOutputList(serviceInfoUpdated)

    assert(tableOutput.toList.exists(out =>
      out.contains("3001/udp") &&
        !out.contains("3000/tcp") &&
        !out.contains("3002/tcp")))
  }

  def getComposeMock(
    composeFileName: String,
    serviceName: String = "testservice",
    versionNumber: String = "1.0.0",
    instanceName: String = "123456",
    containerId: String = "containerId01",
    dockerMachineName: String = "default",
    containerHost: String = "192.168.99.10",
    noBuild: Boolean = false
  ): (DockerComposePluginLocal, String) = {
    val composeMock = spy(new DockerComposePluginLocal)

    val composeFilePath = getClass.getResource(composeFileName).getPath
    doReturn(composeFilePath).when(composeMock).getSetting(composeFile)(null)

    doReturn(serviceName).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(versionNumber).when(composeMock).getSetting(version)(null)
    doReturn(noBuild).when(composeMock).getSetting(composeNoBuild)(null)
    doReturn(containerId).when(composeMock).getDockerContainerId(instanceName, serviceName)
    doReturn(containerHost).when(composeMock).getContainerHost(dockerMachineName, instanceName, null)
    doReturn("").when(composeMock).getDockerContainerInfo(containerId)
    doReturn("").when(composeMock).dockerMachineIp(dockerMachineName)

    (composeMock, composeFilePath)
  }
}
