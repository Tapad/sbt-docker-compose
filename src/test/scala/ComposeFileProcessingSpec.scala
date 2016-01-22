import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerComposePlugin._
import com.tapad.docker.{ ComposeFile, DockerComposePluginLocal }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }
import sbt.Keys._

class ComposeFileProcessingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockitoSugar {

  test("Validate Compose <localBuild> tag results in 'build' image source and custom tag is removed") {
    val composeMock = getComposeFileMock(serviceName = "nomatch")
    val composeFilePath = getClass.getResource("localbuild_tag.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose <skipPull> tag results in 'cache' image source and custom tag is removed") {
    val composeMock = getComposeFileMock(serviceName = "nomatch")
    val composeFilePath = getClass.getResource("skippull_tag.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == cachedImageSource))
  }

  test("Validate Compose service name match results in 'build' image source") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("no_custom_tags.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose no matching compose service name results in 'defined' image source") {
    val composeMock = getComposeFileMock(serviceName = "nomatch")
    val composeFilePath = getClass.getResource("no_custom_tags.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == definedImageSource))
  }

  test("Validate Compose 'composeNoBuild' setting results in image source 'defined' even if service matches the composeServiceName") {
    val composeMock = getComposeFileMock(noBuild = true)
    val composeFilePath = getClass.getResource("no_custom_tags.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == definedImageSource))
  }

  test("Validate Compose 'skipPull' command line argument results in all non-build image sources as 'cache'") {
    val composeFilePath = getClass.getResource("multi_service_no_tags.yml").getPath
    val composeMock = getComposeFileMock(serviceName = "nomatch")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq(skipPullArg), composeYaml)

    assert(serviceInfo.size == 2)
    assert(serviceInfo.forall(_.imageSource == cachedImageSource))
  }

  test("Validate Compose 'skipPull' command line argument still leaves the matching compose image as 'build'") {
    val newServiceName = "testservice1"
    val composeMock = getComposeFileMock(serviceName = newServiceName)
    val composeFilePath = getClass.getResource("multi_service_no_tags.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq(skipPullArg), composeYaml)

    assert(serviceInfo.size == 2)
    assert(serviceInfo.exists(service => service.imageName == s"$newServiceName:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose 'skipBuild' command line argument still leaves the matching compose image as 'build'") {
    val newServiceName = "testservice1"
    val composeMock = getComposeFileMock(serviceName = newServiceName)
    val composeFilePath = getClass.getResource("multi_service_no_tags.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq(skipPullArg), composeYaml)

    assert(serviceInfo.size == 2)
    assert(serviceInfo.exists(service => service.imageName == s"$newServiceName:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose image version number is updated to sbt 'version' when not blank or defined as 'latest'") {
    val newVersion = "9.9.9"
    val composeMock = getComposeFileMock(versionNumber = newVersion)
    val composeFilePath = getClass.getResource("version_number.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == s"testservice:$newVersion" && service.imageSource == buildImageSource))
  }

  test("Validate the correct Debug port is found when supplied in the 'host:container' format") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("debug_port.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug) &&
      service.ports.exists(_.containerPort == "5005")))
  }

  test("Validate the correct Debug port is found when supplied in the 'host' format") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("debug_port_single.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug) &&
      service.ports.exists(_.containerPort == "5005")))
  }

  test("Validate the Debug port is not found when no exposed") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("debug_port_not_exposed.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug == false) &&
      service.ports.exists(_.containerPort == "1234")))
  }

  def getComposeFileMock(serviceName: String = "testservice", versionNumber: String = "1.0.0", noBuild: Boolean = false): ComposeFile = {
    val composeMock = spy(new DockerComposePluginLocal)

    doReturn(serviceName).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(versionNumber).when(composeMock).getSetting(version)(null)
    doReturn(noBuild).when(composeMock).getSetting(composeNoBuild)(null)

    composeMock
  }
}