import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker._
import net.liftweb.json.JValue
import org.mockito.Mockito._
import org.mockito.Matchers._
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

    assert(tableOutput.toList.exists(out =>
      out.hostWithPort.contains("none")
        && out.containerPort == "<none>"))
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
    val expectedContainerPorts = List("3000", "3001/udp", "3002")
    assert(tableOutput.toList.map(_.containerPort) == expectedContainerPorts)
  }

  test("Validate the table output is sorted by service name, then by isDebug, and last by container ports, all ascending") {
    // Set up Compose Mock with Port Mappings
    val (composeMock, composeFilePath) = getComposeMock(
      "sort.yml",
      serviceNames = List("testserviceB", "testserviceA"),
      containerIds = List("containerId02", "containerId01")
    )
    val container1PortMappings =
      """5005/tcp -> 0.0.0.0:32803
        |2003/tcp -> 0.0.0.0:32804
        |12345/tcp -> 0.0.0.0:32805""".stripMargin
    val container2PortMappings =
      """5005/tcp -> 0.0.0.0:32806
        |80/tcp -> 0.0.0.0:32807
        |10000/tcp -> 0.0.0.0:32808
        |8000/udp -> 0.0.0.0:32809""".stripMargin
    doReturn(container1PortMappings).when(composeMock).getDockerPortMappings("containerId01")
    doReturn(container2PortMappings).when(composeMock).getDockerPortMappings("containerId02")

    // Get a collection of ServiceInfo from docker-compose file (sort.xml)
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq.empty, composeYaml)
    val serviceInfoUpdated = composeMock.populateServiceInfoForInstance("123456", "default", serviceInfo, 1000)

    // Sort the Table Output Rows
    val tableOutput = composeMock.getTableOutputList(serviceInfoUpdated)
    val tableOutputSorted = tableOutput.toList.sorted

    // Extract the relevant columns (service name, container port, isDebug) and compare against hard-coded expectations.
    val expPartialTable = List(
      ("testserviceA", "2003", false),
      ("testserviceA", "12345", false),
      ("testserviceA", "5005", true),
      ("testserviceB", "80", false),
      ("testserviceB", "8000/udp", false),
      ("testserviceB", "10000", false),
      ("testserviceB", "5005", true)
    )
    val actPartialTable = tableOutputSorted.map(row => (row.serviceName, row.containerPort, row.isDebug))
    assert(actPartialTable == expPartialTable)
  }

  def getComposeMock(
    composeFileName: String,
    serviceNames: List[String] = List("testservice"),
    versionNumber: String = "1.0.0",
    instanceName: String = "123456",
    containerIds: List[String] = List("containerId01"),
    dockerMachineName: String = "default",
    containerHost: String = "192.168.99.10",
    noBuild: Boolean = false
  ): (DockerComposePluginLocal, String) = {

    require(serviceNames.length == containerIds.length)

    val composeMock = spy(new DockerComposePluginLocal)
    val composeFilePath = getClass.getResource(composeFileName).getPath

    doReturn(composeFilePath).when(composeMock).getSetting(composeFile)(null)
    doReturn(serviceNames.head).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(versionNumber).when(composeMock).getSetting(version)(null)
    doReturn(noBuild).when(composeMock).getSetting(composeNoBuild)(null)
    doReturn(containerHost).when(composeMock).getContainerHost(any[String], any[String], any[JValue])

    serviceNames.zip(containerIds).foreach {
      case (serviceName, containerId) =>
        doReturn(containerId).when(composeMock).getDockerContainerId(instanceName, serviceName)
        doReturn("").when(composeMock).getDockerContainerInfo(containerId)
    }

    (composeMock, composeFilePath)
  }
}
