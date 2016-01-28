package com.tapad.docker

import java.io.{ File, FileWriter }

import com.tapad.docker.ComposeFile._
import com.tapad.docker.DockerComposeKeys._
import org.yaml.snakeyaml.Yaml
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.collection.{ Iterable, Seq }
import scala.io.Source._

object ComposeFile {
  val imageKey = "image"
  val environmentKey = "environment"
  val portsKey = "ports"
  val useLocalBuildTag = "<localbuild>"
  val skipPullTag = "<skippull>"
  type yamlData = Map[String, java.util.LinkedHashMap[String, Any]]
}

trait ComposeFile extends SettingsHelper {
  import com.tapad.docker.DockerComposePlugin._

  /**
   * processCustomTags performs any pre-processing of Custom Tags in the Compose File before the Compose file is used by Docker. This function will also determine any debug ports.
   * This function can be overridden in derived plug-ins to add additional custom tags to process
   * @param state The sbt state
   * @param args Args passed to sbt command
   * @return The collection of ServiceInfo objects. The Compose Yaml passed in is also modified inplace so the calling function will have the updates performed here
   */
  def processCustomTags(implicit state: State, args: Seq[String], composeYaml: yamlData): Iterable[ServiceInfo] = {
    val useExistingImages = getSetting(composeNoBuild)
    val localService = getSetting(composeServiceName)

    composeYaml.map { service =>
      val (serviceName, serviceData) = service match { case (a, b) => (a, b) }
      val imageName = serviceData.get(imageKey).toString

      //Update Compose yaml with any images built as part of dockerComposeUp regardless of how it's defined in the compose file
      val (updatedImageName, imageSource) = if (!useExistingImages && serviceName == localService) {
        //If the image does not contain a tag or has the tag "latest" it will not be replaced
        (replaceDefinedVersionTag(imageName, getSetting(version)), buildImageSource)
      } else if (imageName.toLowerCase.contains(useLocalBuildTag)) {
        (processImageTag(state, args, imageName), buildImageSource)
      } else if (imageName.toLowerCase.contains(skipPullTag) || containsArg(skipPullArg, args)) {
        (processImageTag(state, args, imageName), cachedImageSource)
      } else {
        (imageName, definedImageSource)
      }

      serviceData.put(imageKey, updatedImageName)
      ServiceInfo(serviceName, updatedImageName, imageSource, getPortInfo(serviceData))
    }
  }

  /**
   * Function that reads plug-in defined "<customTag>" fields from the Docker Compose file and performs some transformation on the Docker File based on the tag.
   * The file after transformations are applied is what is used by Docker Compose to launch the instance. This function can be overridden in derived plug-ins to add
   * additional tags pre-processing features.
   * @param state The sbt state
   * @param args Args passed to sbt command
   * @param imageName The image name and tag to be processed for example "testimage:1.0.0<skipPull>" This plugin just removes the tags from the image name.
   * @return The updated image value after any processing indicated by the custom tags
   */
  def processImageTag(implicit state: State, args: Seq[String], imageName: String): String = {
    imageName.replaceAll(s"(?i)$useLocalBuildTag", "").replaceAll(s"(?i)$skipPullTag", "")
  }

  /**
   * Parses the Port information from the Yaml content for a service. It will also report any ports that are exposed as Debugging ports
   * @param serviceKeys The Docker Compose Yaml representing a service
   * @return PortInfo collection for all defined ports
   */
  def getPortInfo(serviceKeys: java.util.LinkedHashMap[String, Any]): List[PortInfo] = {
    if (serviceKeys.containsKey(portsKey)) {
      //Determine if there is a debug port set on the service
      val debugPort = if (serviceKeys.containsKey(environmentKey)) {
        val env = serviceKeys.get(environmentKey).asInstanceOf[java.util.LinkedHashMap[String, String]].asScala
        val debugOptions = env.filter(_._1 == "JAVA_TOOL_OPTIONS")
        val debugAddress = debugOptions.flatMap(_._2.split(',')).filter(_.contains("address")).mkString.split("=")
        if (debugAddress.size == 2) debugAddress(1) else "none"
      }

      serviceKeys.get(portsKey).asInstanceOf[java.util.ArrayList[String]].asScala.map(port => {
        val portArray = port.split(':')
        val (hostPort, containerPort) = if (portArray.length == 2) (portArray(0), portArray(1)) else (portArray(0), portArray(0))
        val debugMatch = portArray.contains(debugPort)
        PortInfo(hostPort, containerPort, debugMatch)
      }).toList
    } else List.empty
  }

  def readComposeFile(composePath: String): yamlData = {
    new Yaml().load(fromFile(composePath).reader()).asInstanceOf[java.util.Map[String, java.util.LinkedHashMap[String, Any]]].asScala.toMap
  }

  def deleteComposeFile(composePath: String): Unit = {
    new File(composePath).delete()
  }

  /**
   * Saves the supplied Docker Compose Yaml data to a temporary file
   * @param finalYaml Compose Yaml to save
   * @return The path to the temporary Compose File
   */
  def saveComposeFile(finalYaml: yamlData): String = {
    val tmp = File.createTempFile("compose-updated", ".yml")
    val updatedComposePath = tmp.getPath
    val writer = new FileWriter(updatedComposePath)
    try {
      new Yaml().dump(finalYaml.asJava, writer)
    } finally {
      writer.close()
    }

    updatedComposePath
  }
}