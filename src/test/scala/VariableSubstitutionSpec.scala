import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerComposePlugin._
import com.tapad.docker.{ Version, RunningInstanceInfo, DockerComposePluginLocal }
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }
import sbt.State

class VariableSubstitutionSpec extends FunSuite with OneInstancePerTest with MockHelpers {
  test("Validate that provided variables are passed to docker-compose up command") {
    val composeMock = spy(new DockerComposePluginLocal)
    val serviceName = "matchingservice"

    val variables = Map("foo" -> "bar")
    val actualVariables = variables.toVector

    doReturn(variables).when(composeMock).getSetting(variablesForSubstitution)(null)

    doReturn(null).when(composeMock).getSetting(composeFile)(null)
    doReturn(null).when(composeMock).readComposeFile(null)
    doReturn(null).when(composeMock).processCustomTags(null, null, null)
    doReturn(null).when(composeMock).saveComposeFile(null)
    doNothing().when(composeMock).pullDockerImages(null, null)
    doReturn(null).when(composeMock).generateInstanceName(null)
    doReturn(null).when(composeMock).getRunningInstanceInfo(null, null, null, null, actualVariables)
    doNothing().when(composeMock).printMappedPortInformation(null, null, dockerComposeVersion)
    doReturn(null).when(composeMock).saveInstanceToSbtSession(null, null)

    mockDockerCommandCalls(composeMock)
    mockSystemSettings(composeMock, serviceName, None)

    composeMock.startDockerCompose(null, null)

    verify(composeMock, times(1)).getRunningInstanceInfo(null, null, null, null, actualVariables)
    verify(composeMock, times(1)).dockerComposeUp(null, null, actualVariables)
  }
}
