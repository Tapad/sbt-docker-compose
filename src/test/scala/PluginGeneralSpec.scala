import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerComposePlugin._
import com.tapad.docker._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }

import scala._
import scala.io._

class PluginGeneralSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockitoSugar {

  test("Validate containsArg function") {
    val plugin = new DockerComposePluginLocal
    assert(!plugin.containsArg(skipPullArg, Seq("")))
    assert(!plugin.containsArg(skipPullArg, Seq(skipBuildArg)))
    assert(plugin.containsArg(skipPullArg, Seq(skipPullArg)))
    assert(plugin.containsArg(skipPullArg, Seq(skipPullArg, skipBuildArg)))
  }

  test("Validate timeout when getting running instance info") {
    val serviceName = "service"
    val instanceName = "instance"
    val dockerMachineName = "default"
    val composeMock = spy(new DockerComposePluginLocal)
    doReturn(false).when(composeMock).getSetting(suppressColorFormatting)(null)
    doReturn("").when(composeMock).getDockerContainerId(instanceName, serviceName)

    val serviceInfo = ServiceInfo(serviceName, "image", "source", null)
    val thrown = intercept[IllegalStateException] {
      composeMock.populateServiceInfoForInstance(null, instanceName, dockerMachineName, List(serviceInfo), 0)
    }

    assert(thrown.getMessage === s"Cannot determine container Id for service: $serviceName")
  }

  test("Validate Docker container inspection populates ServiceInfo properly for various port formats") {
    val serviceName = "service"
    val instanceName = "instance"
    val containerId = "123456"
    val dockerMachineName = "default"
    val jsonStream = getClass.getResourceAsStream("docker_inspect.json")
    val dockerPortStream = getClass.getResourceAsStream("docker_port.txt")
    val inspectJson = Source.fromInputStream(jsonStream).mkString
    val portMappings = Source.fromInputStream(dockerPortStream).mkString
    println(portMappings)
    val composeMock = spy(new DockerComposePluginLocal)
    doReturn(containerId).when(composeMock).getDockerContainerId(instanceName, serviceName)
    doReturn(inspectJson).when(composeMock).getDockerContainerInfo(containerId)
    doReturn(portMappings).when(composeMock).getDockerPortMappings(containerId)
    doReturn(false).when(composeMock).isBoot2DockerEnvironment
    doReturn(false).when(composeMock).isDockerForMacEnvironment

    val port1 = PortInfo("0", "3000/tcp", isDebug = false)
    val port2 = PortInfo("0", "3001/udp", isDebug = false)
    val port3 = PortInfo("0", "3002", isDebug = false)
    val serviceInfo = ServiceInfo(serviceName, "image", "source", List(port1, port2, port3))
    val serviceInfoUpdated = composeMock.populateServiceInfoForInstance(null, instanceName, dockerMachineName, List(serviceInfo), 60)

    assert(serviceInfoUpdated.size == 1)
    val portInfo = serviceInfoUpdated.head.ports

    assert(portInfo.size == 3)
    assert(portInfo.exists(port => port.containerPort.contains("3000") && port.hostPort == "32803"))
    assert(portInfo.exists(port => port.containerPort.contains("3001") && port.hostPort == "32802"))
    assert(portInfo.exists(port => port.containerPort.contains("3002") && port.hostPort == "32801"))
  }

  test("Validate Docker Compose 2.0 NetworkSettings are read when available") {
    val serviceName = "service"
    val instanceName = "instance"
    val containerId = "123456"
    val dockerMachineName = "default"
    val jsonStream = getClass.getResourceAsStream("docker_inspect2.0.json")
    val dockerPortStream = getClass.getResourceAsStream("docker_port.txt")
    val inspectJson = Source.fromInputStream(jsonStream).mkString
    val portMappings = Source.fromInputStream(dockerPortStream).mkString
    val composeMock = spy(new DockerComposePluginLocal)
    doReturn(containerId).when(composeMock).getDockerContainerId(instanceName, serviceName)
    doReturn(inspectJson).when(composeMock).getDockerContainerInfo(containerId)
    doReturn(portMappings).when(composeMock).getDockerPortMappings(containerId)
    doReturn(false).when(composeMock).isBoot2DockerEnvironment
    doReturn(false).when(composeMock).isDockerForMacEnvironment

    val port = PortInfo("0", "3002", isDebug = false)
    val serviceInfo = ServiceInfo(serviceName, "image", "source", List(port))
    val serviceInfoUpdated = composeMock.populateServiceInfoForInstance(null, instanceName, dockerMachineName, List(serviceInfo), 60)
    assert(serviceInfoUpdated.head.containerHost == "172.18.0.1")
  }

  test("Validate Docker instance name generation is random") {
    val composeMock = spy(new DockerComposePluginLocal)
    val instanceName1 = "123"
    val instance1 = RunningInstanceInfo(instanceName1, "servicename", "path", List.empty)
    doReturn(Option(List(instance1))).when(composeMock).getAttribute(runningInstances)(null)
    val instanceName2 = composeMock.generateInstanceName(null)

    assert(instanceName1 != instanceName2)
  }
}

trait MockOutput extends PrintFormatting {
  var messages: Seq[String] = Seq()

  override def print(s: String) = messages = messages :+ s
  override def printBold(s: String, noColor: Boolean) = messages = messages :+ s
}
