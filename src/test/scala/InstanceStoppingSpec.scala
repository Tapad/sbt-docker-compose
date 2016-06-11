import com.tapad.docker.{ DockerComposePluginLocal, RunningInstanceInfo }
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }

class InstanceStoppingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockHelpers {
  test("Validate the proper stopping of a single instance when only one instance is running and no specific instances are passed in as arguments") {
    val instanceId = "instanceId"
    val composePath = "path"
    val serviceName = "service"
    val variables = Vector[(String, String)](("foo", "bar"))
    val composeMock = spy(new DockerComposePluginLocal)
    val instance = RunningInstanceInfo(instanceId, serviceName, composePath, List.empty, variables)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance)))

    composeMock.stopRunningInstances(null, Seq.empty)

    //Validate that the instance was stopped and cleaned up
    verify(composeMock, times(1)).dockerComposeStopInstance(instanceId, composePath, variables)
    verify(composeMock, times(1)).dockerComposeRemoveContainers(instanceId, composePath, variables)
  }

  test("Validate the proper stopping of a multiple instances when no specific instances are passed in as arguments") {
    val instanceId = "instanceId"
    val composePath = "path"
    val serviceName = "service"
    val composeMock = spy(new DockerComposePluginLocal)
    val instance = RunningInstanceInfo(instanceId, serviceName, composePath, List.empty)
    val instance2 = RunningInstanceInfo("instanceId2", serviceName, composePath, List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance, instance2)))

    composeMock.stopRunningInstances(null, Seq.empty)

    //Validate that the instance was stopped and cleaned up
    verify(composeMock, times(2)).dockerComposeStopInstance(anyString, anyString, any[Vector[(String, String)]])
    verify(composeMock, times(2)).dockerComposeRemoveContainers(anyString, anyString, any[Vector[(String, String)]])
  }

  test("Validate the proper stopping of a single instance when multiple instances are running") {
    val instanceIdStop = "instanceIdStop"
    val instanceIdKeep = "instanceIdKeep"
    val serviceName = "service"
    val composePath = "path"
    val composeMock = spy(new DockerComposePluginLocal)
    val instanceStop = RunningInstanceInfo(instanceIdStop, serviceName, composePath, List.empty)
    val instanceKeep = RunningInstanceInfo(instanceIdKeep, serviceName, composePath, List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instanceStop, instanceKeep)))

    composeMock.stopRunningInstances(null, Seq(instanceIdStop))

    //Validate that only once instance was Stopped and Removed
    verify(composeMock, times(1)).setAttribute(any, any)(any[sbt.State])
    verify(composeMock, times(1)).dockerComposeStopInstance(anyString, anyString, any[Vector[(String, String)]])
    verify(composeMock, times(1)).dockerComposeRemoveContainers(anyString, anyString, any[Vector[(String, String)]])
  }

  test("Validate that only instances from the current SBT project are stopped when no arguments are supplied to DockerComposeStop") {
    val composeMock = spy(new DockerComposePluginLocal)
    val serviceName = "matchingservice"
    val instance1 = RunningInstanceInfo("instanceName1", serviceName, "path", List.empty)
    val instance2 = RunningInstanceInfo("instanceName2", serviceName, "path", List.empty)
    val instance3 = RunningInstanceInfo("instanceName3", "nonSbtProjectService", "path", List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance1, instance2, instance3)))

    composeMock.stopRunningInstances(null, Seq.empty)

    //Validate that only once instance was Stopped and Removed
    verify(composeMock, times(1)).setAttribute(any, any)(any[sbt.State])
    verify(composeMock, times(2)).dockerComposeStopInstance(anyString, anyString, any[Vector[(String, String)]])
    verify(composeMock, times(2)).dockerComposeRemoveContainers(anyString, anyString, any[Vector[(String, String)]])
  }

  test("Validate that instances from any SBT project can be stopped when explicitly passed to DockerComposeStop") {
    val composeMock = spy(new DockerComposePluginLocal)
    val serviceName = "matchingservice"
    val instance1 = RunningInstanceInfo("instanceName1", serviceName, "path", List.empty)
    val instance2 = RunningInstanceInfo("instanceName2", serviceName, "path", List.empty)
    val instance3 = RunningInstanceInfo("instanceName3", "nonSbtProjectService", "path", List.empty)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, Some(List(instance1, instance2, instance3)))

    composeMock.stopRunningInstances(null, Seq("instanceName3"))

    //Validate that only once instance was Stopped and Removed
    verify(composeMock, times(1)).setAttribute(any, any)(any[sbt.State])
    verify(composeMock, times(1)).dockerComposeStopInstance(anyString, anyString, any[Vector[(String, String)]])
    verify(composeMock, times(1)).dockerComposeRemoveContainers(anyString, anyString, any[Vector[(String, String)]])
  }
}
