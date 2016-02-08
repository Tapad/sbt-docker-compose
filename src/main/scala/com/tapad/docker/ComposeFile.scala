package com.tapad.docker

import java.io.{ File, FileWriter }

import com.tapad.docker.DockerComposeKeys._
import org.yaml.snakeyaml.Yaml
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.collection.{ Iterable, Seq }
import scala.io.Source._

trait ComposeFile extends SettingsHelper with ComposeCustomTagHelpers {
  // Compose file Yaml keys
  val imageKey = "image"
  val environmentKey = "environment"
  val portsKey = "ports"
  val servicesKey = "services"

  //Set of values representing the source location of a Docker Compose image
  val cachedImageSource = "cache"
  val definedImageSource = "defined"
  val buildImageSource = "build"

  //Custom tags
  val useLocalBuildTag = "<localbuild>"
  val skipPullTag = "<skippull>"

  type yamlData = Map[String, java.util.LinkedHashMap[String, Any]]

  /**
   * processCustomTags performs any pre-processing of Custom Tags in the Compose File before the Compose file is used
   * by Docker. This function will also determine any debug ports.
   * This function can be overridden in derived plug-ins to add additional custom tags to process
   * @param state The sbt state
   * @param args Args passed to sbt command
   * @return The collection of ServiceInfo objects. The Compose Yaml passed in is also modified in-place so the calling
   *        function will have the updates performed here
   */
  def processCustomTags(implicit state: State, args: Seq[String], composeYaml: yamlData): Iterable[ServiceInfo] = {
    val useExistingImages = getSetting(composeNoBuild)
    val localService = getSetting(composeServiceName)

    getComposeFileServices(composeYaml).map { service =>
      val (serviceName, serviceData) = service
      val imageName = serviceData.get(imageKey).toString

      //Update Compose yaml with any images built as part of dockerComposeUp regardless of how it's defined in the
      //compose file
      val (updatedImageName, imageSource) = if (!useExistingImages && serviceName == localService) {
        //If the image does not contain a tag or has the tag "latest" it will not be replaced
        (replaceDefinedVersionTag(imageName, getSetting(version)), buildImageSource)
      } else if (imageName.toLowerCase.contains(useLocalBuildTag)) {
        (processImageTag(state, args, imageName), buildImageSource)
      } else if (imageName.toLowerCase.contains(skipPullTag) || containsArg(DockerComposePlugin.skipPullArg, args)) {
        (processImageTag(state, args, imageName), cachedImageSource)
      } else {
        (imageName, definedImageSource)
      }

      serviceData.put(imageKey, updatedImageName)
      ServiceInfo(serviceName, updatedImageName, imageSource, getPortInfo(serviceData))
    }
  }

  /**
   * If the Yaml is in the Docker 1.6 format which includes a new "services" key work with that sub-set of data.
   * Otherwise, return the original Yaml
   * @param composeYaml Docker Compose yaml to process
   * @return The 'services' section of the Yaml file
   */
  def getComposeFileServices(composeYaml: yamlData): yamlData = {
    composeYaml.get(servicesKey) match {
      case Some(services) => services.asInstanceOf[java.util.Map[String, java.util.LinkedHashMap[String, Any]]].
        asScala.toMap
      case None => composeYaml
    }
  }

  /**
   * Function that reads plug-in defined "<customTag>" fields from the Docker Compose file and performs some
   * transformation on the Docker File based on the tag. The file after transformations are applied is what is used by
   * Docker Compose to launch the instance. This function can be overridden in derived plug-ins to add additional tags
   * pre-processing features.
   * @param state The sbt state
   * @param args Args passed to sbt command
   * @param imageName The image name and tag to be processed for example "testimage:1.0.0<skipPull>" This plugin just
   *                 removes the tags from the image name.
   * @return The updated image value after any processing indicated by the custom tags
   */
  def processImageTag(implicit state: State, args: Seq[String], imageName: String): String = {
    imageName.replaceAll(s"(?i)$useLocalBuildTag", "").replaceAll(s"(?i)$skipPullTag", "")
  }

  /**
   * Parses the Port information from the Yaml content for a service. It will also report any ports that are exposed as
   * Debugging ports
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
    } else {
      List.empty
    }
  }

  def readComposeFile(composePath: String): yamlData = {
    val fileReader = fromFile(composePath).reader()
    try {
      new Yaml().load(fileReader).asInstanceOf[java.util.Map[String, java.util.LinkedHashMap[String, Any]]].asScala.toMap
    } finally {
      fileReader.close()
    }
  }

  def deleteComposeFile(composePath: String): Boolean = {
    new File(composePath).delete()
  }

  /**
   * Saves the supplied Docker Compose Yaml data to a temporary file
   * @param finalYaml Compose Yaml to save
   * @return The path to the temporary Compose File
   */
  def saveComposeFile(finalYaml: yamlData): String = {
    val updatedComposePath = File.createTempFile("compose-updated", ".yml").getPath
    val writer = new FileWriter(updatedComposePath)
    try {
      new Yaml().dump(finalYaml.asJava, writer)
    } finally {
      writer.close()
    }

    updatedComposePath
  }
}