import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.{ RunningInstanceInfo, DockerComposePluginLocal }
import org.mockito.Matchers._
import org.mockito.Mockito._

trait MockHelpers {
  def mockSystemSettings(composeMock: DockerComposePluginLocal, serviceName: String, instances: Option[List[RunningInstanceInfo]]): Unit = {
    doReturn(null).when(composeMock).getPersistedState(null)
    doReturn(serviceName).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(true).when(composeMock).getSetting(composeRemoveContainersOnShutdown)(null)
    doReturn(false).when(composeMock).getSetting(composeRemoveTempFileOnShutdown)(null)
    doReturn(instances).when(composeMock).getAttribute(runningInstances)(null)
    doReturn(null).when(composeMock).removeAttribute(runningInstances)(null)
    doReturn(null).when(composeMock).setAttribute(any, any)(any[sbt.State])
    doNothing().when(composeMock).saveInstanceState(null)
  }

  /**
   * Stubs out calls to Docker so that they don't actually call any Docker commands
   * @param composeMock Mock instance of the Plugin
   */
  def mockDockerCommandCalls(composeMock: DockerComposePluginLocal): Unit = {
    doNothing().when(composeMock).dockerComposeRemoveContainers(anyString, anyString)
    doNothing().when(composeMock).dockerComposeStopInstance(anyString, anyString)
  }
}
