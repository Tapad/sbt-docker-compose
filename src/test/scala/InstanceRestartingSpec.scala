import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{ FunSuite, OneInstancePerTest }
import scala.collection.Iterable

class InstanceRestartingSpec extends FunSuite with OneInstancePerTest with MockHelpers {

  test("Validate the correct type of exception thrown and error message shown when restarting a non existent instance") {
    val instanceId1 = "123456"
    val instanceId2 = "987654"
    val instanceIdNonExistent = "111111"
    val serviceName = "service"
    val composePath = "path"

    val instance1 = RunningInstanceInfo(instanceId1, serviceName, composePath, List.empty)
    val instance2 = RunningInstanceInfo(instanceId2, serviceName, composePath, List.empty)

    val composeMock = getComposeMock(serviceName)
    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance1, instance2)))

    val thrown = intercept[IllegalArgumentException] {
      composeMock.restartInstancePrecheck(null, Seq(instanceIdNonExistent))
    }

    assert(thrown.getMessage == "No local Docker Compose instances found to restart from current sbt project.")
  }

  test("Validate the proper restarting of a particular instance when multiple instances are running") {
    val instanceIdStop = "123456"
    val instanceIdKeep = "987654"
    val instanceIdLaunch = "111111"
    val serviceName = "service"
    val composePath = "path"

    val composeMock = getComposeMock(serviceName)

    val instanceStop = RunningInstanceInfo(instanceIdStop, serviceName, composePath, List.empty)
    val instanceKeep = RunningInstanceInfo(instanceIdKeep, serviceName, composePath, List.empty)
    val instanceLaunch = RunningInstanceInfo(instanceIdLaunch, serviceName, composePath, List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instanceStop, instanceKeep)))

    doReturn(instanceLaunch).when(composeMock).getRunningInstanceInfo(any[sbt.State], anyString, anyString, any[Iterable[ServiceInfo]])
    composeMock.restartRunningInstance(null, Seq(instanceIdStop))

    //Validate that only one instance is stopped and removed
    verify(composeMock, times(1)).dockerComposeStopInstance(anyString, anyString)
    verify(composeMock, times(1)).dockerComposeRemoveContainers(anyString, anyString)

    //Validate that a new instance is started
    verify(composeMock, times(1)).dockerComposeUp(anyString, anyString)
  }

  test("Validate the proper restarting of an instance when only one instance is running and no instance id is specified") {
    val instanceIdStop = "123456"
    val instanceIdLaunch = "111111"
    val serviceName = "service"
    val composePath = "path"

    val composeMock = getComposeMock(serviceName)

    val instanceStop = RunningInstanceInfo(instanceIdStop, serviceName, composePath, List.empty)
    val instanceLaunch = RunningInstanceInfo(instanceIdLaunch, serviceName, composePath, List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instanceStop)))

    doReturn(instanceLaunch).when(composeMock).getRunningInstanceInfo(any[sbt.State], anyString, anyString, any[Iterable[ServiceInfo]])
    composeMock.restartRunningInstance(null, Seq.empty)

    //Validate that only one instance is stopped and removed
    verify(composeMock, times(1)).dockerComposeStopInstance(anyString, anyString)
    verify(composeMock, times(1)).dockerComposeRemoveContainers(anyString, anyString)

    //Validate that a new instance is started
    verify(composeMock, times(1)).dockerComposeUp(anyString, anyString)
  }

  test("Validate the correct type of exception thrown and error message shown when multiple instances are running and no instance id is specified") {
    val instanceId1 = "123456"
    val instanceId2 = "987654"
    val serviceName = "service"
    val composePath = "path"

    val composeMock = getComposeMock(serviceName)

    val instance1 = RunningInstanceInfo(instanceId1, serviceName, composePath, List.empty)
    val instance2 = RunningInstanceInfo(instanceId2, serviceName, composePath, List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance1, instance2)))

    val thrown = intercept[IllegalArgumentException] {
      composeMock.restartInstancePrecheck(null, Seq.empty)
    }

    assert(thrown.getMessage == "More than one running instance from the current sbt project was detected. " +
      "Please provide an Instance Id parameter to the dockerComposeRestart command specifying which instance to stop.")
  }

  test("Validate the proper starting of a new instance when there is no running instance to restart") {
    val instanceIdLaunch = "111111"
    val serviceName = "service"
    val composePath = "path"

    val composeMock = getComposeMock(serviceName)

    val instanceLaunch = RunningInstanceInfo(instanceIdLaunch, serviceName, composePath, List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List.empty))

    doReturn(instanceLaunch).when(composeMock).getRunningInstanceInfo(any[sbt.State], anyString, anyString, any[Iterable[ServiceInfo]])
    composeMock.restartRunningInstance(null, Seq.empty)

    //Validate that a new instance is started
    verify(composeMock, times(1)).dockerComposeUp(anyString, anyString)
  }

  def getComposeMock(serviceName: String = "testservice"): DockerComposePluginLocal = {
    val composeMock = spy(new DockerComposePluginLocal)

    val composeFilePath = getClass.getResource("debug_port.yml").getPath
    doReturn(composeFilePath).when(composeMock).getSetting(composeFile)(null)
    doReturn(serviceName).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(true).when(composeMock).getSetting(composeNoBuild)(null)
    doReturn(Vector.empty).when(composeMock).runVariablesForSubstitutionTask(null)
    doReturn(Map.empty).when(composeMock).getSetting(variablesForSubstitution)(null)
    doReturn(0).when(composeMock).dockerComposeUp(anyString, anyString)
    doNothing().when(composeMock).pullDockerImages(any[Seq[String]], any[Iterable[ServiceInfo]])
    doNothing().when(composeMock).buildDockerImageTask(null)

    composeMock
  }
}
