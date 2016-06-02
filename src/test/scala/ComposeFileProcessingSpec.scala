import java.io.File
import java.util

import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerComposePlugin._
import com.tapad.docker.{ ComposeFile, ComposeFileFormatException, DockerComposePluginLocal }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfter, FunSuite, OneInstancePerTest }
import sbt.Keys._

class ComposeFileProcessingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockitoSugar {

  test("Validate Compose field 'build:' results in correct exception thrown and error message printing") {
    val (composeMock, composeFilePath) = getComposeFileMock("unsupported_field_build.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)

    val thrown = intercept[ComposeFileFormatException] {
      composeMock.processCustomTags(null, null, composeYaml)
    }
    assert(thrown.getMessage == getUnsupportedFieldErrorMsg("build"))
  }

  test("Validate Compose field 'container_name:' results in correct exception thrown and error message printing") {
    val (composeMock, composeFilePath) = getComposeFileMock("unsupported_field_container_name.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)

    val thrown = intercept[ComposeFileFormatException] {
      composeMock.processCustomTags(null, null, composeYaml)
    }
    assert(thrown.getMessage == getUnsupportedFieldErrorMsg("container_name"))
  }

  test("Validate Compose field 'extends:' results in correct exception thrown and error message printing") {
    val (composeMock, composeFilePath) = getComposeFileMock("unsupported_field_extends.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)

    val thrown = intercept[ComposeFileFormatException] {
      composeMock.processCustomTags(null, null, composeYaml)
    }
    assert(thrown.getMessage == getUnsupportedFieldErrorMsg("extends"))
  }

  test("Validate Compose <localBuild> tag results in 'build' image source and custom tag is removed") {
    val (composeMock, composeFilePath) = getComposeFileMock("localbuild_tag.yml", serviceName = "nomatch")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose <skipPull> tag results in 'cache' image source and custom tag is removed") {
    val (composeMock, composeFilePath) = getComposeFileMock("skippull_tag.yml", serviceName = "nomatch")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == cachedImageSource))
  }

  test("Validate Compose service name match results in 'build' image source") {
    val (composeMock, composeFilePath) = getComposeFileMock("no_custom_tags.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose no matching compose service name results in 'defined' image source") {
    val (composeMock, composeFilePath) = getComposeFileMock("no_custom_tags.yml", serviceName = "nomatch")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == definedImageSource))
  }

  test("Validate Compose 'composeNoBuild' setting results in image source 'defined' even if service matches the composeServiceName") {
    val (composeMock, composeFilePath) = getComposeFileMock("no_custom_tags.yml", noBuild = true)
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" && service.imageSource == definedImageSource))
  }

  test("Validate Compose 'skipPull' command line argument results in all non-build image sources as 'cache'") {
    val (composeMock, composeFilePath) = getComposeFileMock("multi_service_no_tags.yml", serviceName = "nomatch")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq(skipPullArg), composeYaml)

    assert(serviceInfo.size == 2)
    assert(serviceInfo.forall(_.imageSource == cachedImageSource))
  }

  test("Validate Compose 'skipPull' command line argument still leaves the matching compose image as 'build'") {
    val newServiceName = "testservice1"
    val (composeMock, composeFilePath) = getComposeFileMock("multi_service_no_tags.yml", serviceName = newServiceName)
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq(skipPullArg), composeYaml)

    assert(serviceInfo.size == 2)
    assert(serviceInfo.exists(service => service.imageName == s"$newServiceName:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose 'skipBuild' command line argument still leaves the matching compose image as 'build'") {
    val newServiceName = "testservice1"
    val (composeMock, composeFilePath) = getComposeFileMock("multi_service_no_tags.yml", serviceName = newServiceName)
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, Seq(skipPullArg), composeYaml)

    assert(serviceInfo.size == 2)
    assert(serviceInfo.exists(service => service.imageName == s"$newServiceName:latest" && service.imageSource == buildImageSource))
  }

  test("Validate Compose image version number is updated to sbt 'version' when not blank or defined as 'latest'") {
    val newVersion = "9.9.9"
    val (composeMock, composeFilePath) = getComposeFileMock("version_number.yml", versionNumber = newVersion)
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == s"testservice:$newVersion" && service.imageSource == buildImageSource))
  }

  test("Validate the correct Debug port is found when supplied in the 'host:container' format") {
    val (composeMock, composeFilePath) = getComposeFileMock("debug_port.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug) &&
      service.ports.exists(_.containerPort == "5005")))
  }

  test("Validate the correct Debug port is found when supplied in the 'host' format") {
    val (composeMock, composeFilePath) = getComposeFileMock("debug_port_single.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug) &&
      service.ports.exists(_.containerPort == "5005")))
  }

  test("Validate the correct Debug port is found when the alternate 'environment:' field format is used") {
    val (composeMock, composeFilePath) = getComposeFileMock("debug_port_alternate_environment_format.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug) &&
      service.ports.exists(_.containerPort == "5005")))
  }

  test("Validate the Debug port is not found when not exposed") {
    val (composeMock, composeFilePath) = getComposeFileMock("debug_port_not_exposed.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "testservice:latest" &&
      service.ports.exists(_.isDebug == false) &&
      service.ports.exists(_.containerPort == "1234")))
  }

  test("Validate that the 1.6 Docker Compose file format can be properly processed") {
    val (composeMock, composeFilePath) = getComposeFileMock("compose_1.6_format.yml", serviceName = "nomatch")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    val serviceInfo = composeMock.processCustomTags(null, null, composeYaml)

    assert(serviceInfo.exists(service => service.imageName == "myapp:latest" && service.imageSource == cachedImageSource))
    assert(serviceInfo.exists(service => service.imageName == "redis:latest" && service.imageSource == buildImageSource))
    assert(serviceInfo.exists(service => service.imageName == "test:latest" && service.imageSource == definedImageSource))
  }

  test("Validate that Docker Compose file port ranges are expanded") {
    val (composeMock, composeFilePath) = getComposeFileMock("port_expansion.yml")
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
    val (composeMock, composeFilePath) = getComposeFileMock("port_expansion_invalid.yml")
    val composeYaml = composeMock.readComposeFile(composeFilePath)
    intercept[IllegalStateException] {
      composeMock.processCustomTags(null, null, composeYaml)
    }
  }

  test("Validate that the env_file settings gets updated with the fully qualified path") {
    val (composeMock, composeFilePath) = getComposeFileMock("env_file.yml")
    val composeFileDir = composeFilePath.substring(0, composeFilePath.lastIndexOf(File.separator))

    val composeYaml = composeMock.readComposeFile(composeFilePath)
    composeMock.processCustomTags(null, null, composeYaml)
    val modifiedEnvPath = composeYaml.filter(_._1 == "testservice").head._2.get(composeMock.envFileKey)
    assert(modifiedEnvPath == s"$composeFileDir/test.env")

    val modifiedEnvPath2 = composeYaml.filter(_._1 == "testservice2").head._2.get(composeMock.envFileKey).asInstanceOf[util.List[String]]
    assert(modifiedEnvPath2.size() == 2)
    assert(modifiedEnvPath2.get(0) == s"$composeFileDir/test.env")
    assert(modifiedEnvPath2.get(1) == s"$composeFileDir/test2.env")
  }

  test("Validate that an env_file file that cannot be found fails with an exception") {
    val (composeMock, composeFilePath) = getComposeFileMock("env_file_invalid.yml")
    doReturn(composeFilePath).when(composeMock).getSetting(composeFile)(null)

    val composeYaml = composeMock.readComposeFile(composeFilePath)
    intercept[IllegalStateException] {
      composeMock.processCustomTags(null, null, composeYaml)
    }
  }

  test("Validate that docker-compose variables are substituted") {
    val (composeMock, composeFilePath) = getComposeFileMock("variable_substitution.yml")
    doReturn(composeFilePath).when(composeMock).getSetting(composeFile)(null)

    val composeYaml = composeMock.readComposeFile(composeFilePath, Vector(("SOURCE_PORT", "5555")))

    val ports = composeYaml.get("testservice").get.get("ports").asInstanceOf[util.ArrayList[String]].get(0)
    assert(ports == "5555:5005")
  }

  /**
   * @return tuple of a mocked ComposeFile, and the path to that file
   */
  def getComposeFileMock(
    composeFileName: String,
    serviceName: String = "testservice",
    versionNumber: String = "1.0.0",
    noBuild: Boolean = false
  ): (ComposeFile, String) = {
    val composeMock = spy(new DockerComposePluginLocal)

    val composeFilePath = getClass.getResource(composeFileName).getPath
    doReturn(composeFilePath).when(composeMock).getSetting(composeFile)(null)

    doReturn(serviceName).when(composeMock).getSetting(composeServiceName)(null)
    doReturn(versionNumber).when(composeMock).getSetting(version)(null)
    doReturn(noBuild).when(composeMock).getSetting(composeNoBuild)(null)

    (composeMock, composeFilePath)
  }

}
