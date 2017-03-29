import com.tapad.docker.{ RunningInstanceInfo, DockerComposePluginLocal }
import com.tapad.docker.DockerComposeKeys._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }

class InstancePersistenceSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockitoSugar {

  test("Validate that only running instances from this sbt session are returned") {
    val instanceMock = spy(new DockerComposePluginLocal)

    val runningInstanceMatch = RunningInstanceInfo("instanceNameMatch", "matchingservice", "composePath", List.empty)
    val runningInstanceNoMatch = RunningInstanceInfo("instanceNameNoMatch", "nomatchingservice", "composePath", List.empty)

    doReturn("matchingservice").when(instanceMock).getSetting(composeServiceName)(null)
    doReturn(Option(List(runningInstanceMatch, runningInstanceNoMatch))).when(instanceMock).getAttribute(runningInstances)(null)

    val instanceIds = instanceMock.getServiceRunningInstanceIds(null)

    assert(instanceIds.size == 1)
    assert(instanceIds.contains("instanceNameMatch"))
  }

  test("Validate that only matching instance ids are returned") {
    val instanceMock = spy(new DockerComposePluginLocal)

    val runningInstanceMatch = RunningInstanceInfo("instanceNameMatch", "matchingservice", "composePath", List.empty)
    val runningInstanceNoMatch = RunningInstanceInfo("instanceNameNoMatch", "nomatchingservice", "composePath", List.empty)

    doReturn("matchingservice").when(instanceMock).getSetting(composeServiceName)(null)
    doReturn(Option(List(runningInstanceMatch, runningInstanceNoMatch))).when(instanceMock).getAttribute(runningInstances)(null)

    val instance = instanceMock.getMatchingRunningInstance(null, Seq("instanceNameMatch"))

    assert(instance.isDefined)
    assert(instance.get.instanceName == "instanceNameMatch")
  }
}
