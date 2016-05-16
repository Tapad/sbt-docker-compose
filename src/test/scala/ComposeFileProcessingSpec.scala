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

  test("Validate the correct Debug port is found when the alternate 'environment:' field format is used") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("debug_port_alternate_environment_format.yml").getPath
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

  test("Validate that the 1.6 Docker Compose file format can be properly processed") {
    val composeMock = getComposeFileMock(serviceName = "nomatch")
    val composeFilePath = getClass.getResource("compose_1.6_format.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "myapp:latest" && service.imageSource == cachedImageSource))
    assert(serviceInfo.exists(service => service.imageName == "redis:latest" && service.imageSource == buildImageSource))
    assert(serviceInfo.exists(service => service.imageName == "test:latest" && service.imageSource == definedImageSource))
  }

  test("Validate that Docker Compose file port ranges are expanded") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("port_expansion.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service =>
      service.ports.exists(p => p.hostPort == "1000" && p.containerPort == "1000") &&
        service.ports.exists(p => p.hostPort == "2000" && p.containerPort == "2000") &&
        service.ports.exists(p => p.hostPort == "2001" && p.containerPort == "2001") &&
        service.ports.exists(p => p.hostPort == "2002" && p.containerPort == "2002") &&
        service.ports.exists(p => p.hostPort == "3000" && p.containerPort == "3000") &&
        service.ports.exists(p => p.hostPort == "3001" && p.containerPort == "3001") &&
        service.ports.exists(p => p.hostPort == "3002" && p.containerPort == "3002") &&
        service.ports.exists(p => p.hostPort == "5000" && p.containerPort == "4000") &&
        service.ports.exists(p => p.hostPort == "5001" && p.containerPort == "4001") &&
        service.ports.exists(p => p.hostPort == "5002" && p.containerPort == "4002") &&
        service.ports.exists(p => p.hostPort == "6000" && p.containerPort == "6000") &&
        service.ports.exists(p => p.hostPort == "6001" && p.containerPort == "6001")))
  }

  test("Validate that an improper port range throws an exception") {
    val composeMock = getComposeFileMock()
    val composeFilePath = getClass.getResource("port_expansion_invalid.yml").getPath
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    intercept[IllegalStateException] {
      composeMock.processCustomTags(null, null, composeYaml)
    }
  }

  def getComposeFileMock(serviceName: String = "testservice", versionNumber: String = "1.0.0", noBuild: Boolean = false): ComposeFile = {
    val composeMock = spy(new DockerComposePluginLocal)

    doReturn(serviceName).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(versionNumber).when(composeMock).getSetting(version)(null)
    doReturn(noBuild).when(composeMock).getSetting(composeNoBuild)(null)

    composeMock
  }
}