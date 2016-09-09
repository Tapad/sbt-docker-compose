import sbt._
import com.tapad.docker.{DockerComposePluginLocal, RunningInstanceInfo, Version}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite, OneInstancePerTest}

class ComposeInstancesSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockHelpers {
  test("Validate that no instances are printed when none are running") {
    val composeMock = spy(new DockerComposePluginLocal)
    val serviceName = "matchingservice"

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, None)

    composeMock.printDockerComposeInstances(null, null)

    verify(composeMock, times(0)).printMappedPortInformation(any[State], any[RunningInstanceInfo], any[Version])
  }

  test("Validate that multiple instances across sbt projects are printed when they are running") {
    val composeMock = spy(new DockerComposePluginLocal)
    val serviceName = "matchingservice"
    val instance1 = RunningInstanceInfo("instanceName1", serviceName, "path", List.empty)
    val instance2 = RunningInstanceInfo("instanceName2", serviceName, "path", List.empty)
    val instance3 = RunningInstanceInfo("instanceName3", "nonSbtProjectService", "path", List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance1, instance2, instance3)))

    composeMock.printDockerComposeInstances(null, null)

    verify(composeMock, times(3)).printMappedPortInformation(any[State], any[RunningInstanceInfo], any[Version])
  }
}
