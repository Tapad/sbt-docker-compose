import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerComposePlugin._
import com.tapad.docker.DockerComposePluginLocal
import org.mockito.Mockito._
import org.scalatest.{OneInstancePerTest, BeforeAndAfter, FunSuite}

class ImageBuildingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest {
  test("Validate that a Docker image is built when 'skipBuild' and 'noBuild' are not set") {
    val composeMock = spy(new DockerComposePluginLocal)

    doReturn(false).when(composeMock).getSetting(composeNoBuild)(null)
    doNothing().when(composeMock).buildDockerImageTask(null)

    composeMock.buildDockerImage(null, null)

    verify(composeMock, times(1)).buildDockerImageTask(null)
  }

  test("Validate that a Docker image is not built when 'skipBuild' is passed as an argument") {
    val composeMock = spy(new DockerComposePluginLocal)

    doReturn(false).when(composeMock).getSetting(composeNoBuild)(null)
    doNothing().when(composeMock).buildDockerImageTask(null)

    composeMock.buildDockerImage(null, Seq(skipBuildArg))

    verify(composeMock, times(0)).buildDockerImageTask(null)
  }

  test("Validate that a Docker image is not built when the 'noBuild' setting is true") {
    val composeMock = spy(new DockerComposePluginLocal)

    doReturn(true).when(composeMock).getSetting(composeNoBuild)(null)
    doNothing().when(composeMock).buildDockerImageTask(null)

    composeMock.buildDockerImage(null, null)

    verify(composeMock, times(0)).buildDockerImageTask(null)
  }
}
