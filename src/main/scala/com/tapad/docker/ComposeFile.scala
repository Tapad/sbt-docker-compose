package com.tapad.docker

import java.io.{ File, FileWriter }
import java.util

import com.tapad.docker.DockerComposeKeys._
import org.yaml.snakeyaml.Yaml
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.{ Iterable, Seq }
import scala.io.Source._
import scala.util.{ Try, Success, Failure }

trait ComposeFile extends SettingsHelper with ComposeCustomTagHelpers with PrintFormatting {
  // Compose file Yaml keys
  val imageKey = "image"
  val environmentKey = "environment"
  val portsKey = "ports"
  val servicesKey = "services"
  val envFileKey = "env_file"
  val volumesKey = "volumes"

  //Set of values representing the source location of a Docker Compose image
  val cachedImageSource = "cache"
  val definedImageSource = "defined"
  val buildImageSource = "build"

  //Custom tags
  val useLocalBuildTag = "<localbuild>"
  val skipPullTag = "<skippull>"

  val environmentDebugKey = "JAVA_TOOL_OPTIONS"

  //List of docker-compose fields that are currently unsupported by the plugin
  val unsupportedFields = List("build", "container_name", "extends")

  type yamlData = Map[String, java.util.LinkedHashMap[String, Any]]

  /**
   * processCustomTags performs any pre-processing of Custom Tags in the Compose File before the Compose file is used
   * by Docker. This function will also determine any debug ports and rename any 'env_file' defined files to use their
   * fully qualified paths so that they can be accessed from the tmp location the docker-compose.yml is launched from
   * This function can be overridden in derived plug-ins to add additional custom tags to process
   *
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
      for (field <- unsupportedFields if serviceData.containsKey(field)) {
        throw ComposeFileFormatException(getUnsupportedFieldErrorMsg(field))
      }
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

      //Update env_file files to use the fully qualified path so that it can still be accessed from the tmp location
      if (serviceData.containsKey(envFileKey)) {
        val composeFileFullPath = new File(getSetting(composeFile)).getAbsolutePath
        val composeFileDir = composeFileFullPath.substring(0, composeFileFullPath.lastIndexOf(File.separator))

        val entry = serviceData.get(envFileKey)
        entry match {
          case e: String =>
            val updated = getFullyQualifiedPath(e, composeFileDir)
            serviceData.put(envFileKey, updated)
          case e: util.ArrayList[_] =>
            val updated = e.asScala.map(file => getFullyQualifiedPath(file.asInstanceOf[String], composeFileDir))
            serviceData.put(envFileKey, updated.asJava)
        }
      }

      serviceData.put(imageKey, updatedImageName)
      ServiceInfo(serviceName, updatedImageName, imageSource, getPortInfo(serviceData))
    }
  }

  def getUnsupportedFieldErrorMsg(fieldName: String): String = {
    s"Docker Compose field '$fieldName:' is currently not supported by sbt-docker-compose. Please see the README for " +
      s"more information on the set of unsupported fields."
  }

  /**
   *  Attempt to get the fully qualified path to the environment file. It will first attempt to find the file using the
   *  path provided. If that fails it will attempt to find the file relative to the docker-compose yml location. Otherwise,
   *  it will throw an exception with information about the file that could not be located.
   *
   * @param fileName The file name to find
   * @param composePath The path to the directory of the docker-compose yml file being used
   * @return The fully qualified path to the file
   */
  def getFullyQualifiedPath(fileName: String, composePath: String): String = {
    if (new File(fileName).exists) {
      new File(fileName).getAbsolutePath
    } else if (new File(s"$composePath/$fileName").exists) {
      new File(s"$composePath/$fileName").getAbsolutePath
    } else {
      throw new IllegalStateException(s"Could not find env_file: '$fileName' either at the specified path or in the '$composePath' directory.")
    }
  }

  /**
   * If the Yaml is in the Docker 1.6 format which includes a new "services" key work with that sub-set of data.
   * Otherwise, return the original Yaml
   *
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

  def getComposeVersion(composeYaml: yamlData): Int = {
    composeYaml.get(servicesKey) match {
      case Some(services) => 2
      case None => 1
    }
  }

  /**
   * Function that reads plug-in defined "<customTag>" fields from the Docker Compose file and performs some
   * transformation on the Docker File based on the tag. The file after transformations are applied is what is used by
   * Docker Compose to launch the instance. This function can be overridden in derived plug-ins to add additional tags
   * pre-processing features.
   *
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
   * Debugging ports and expand any defined port ranges
   *
   * @param serviceKeys The Docker Compose Yaml representing a service
   * @return PortInfo collection for all defined ports
   */
  def getPortInfo(serviceKeys: java.util.LinkedHashMap[String, Any]): List[PortInfo] = {
    if (serviceKeys.containsKey(portsKey)) {
      //Determine if there is a debug port set on the service
      val debugPort = if (serviceKeys.containsKey(environmentKey)) {
        val debugAddress = {
          serviceKeys.get(environmentKey) match {
            case key: util.LinkedHashMap[_, _] =>
              val env = key.asInstanceOf[java.util.LinkedHashMap[String, String]].asScala
              val debugOptions = env.filter(_._1 == environmentDebugKey)
              debugOptions.flatMap(_._2.split(','))
            case key: util.ArrayList[_] =>
              val env = key.asInstanceOf[util.ArrayList[String]].asScala
              val debugOptions = env.filter(_.startsWith(environmentDebugKey))
              debugOptions.flatMap(_.split(','))
          }
        }.filter(_.contains("address")).mkString.split("=")

        if (debugAddress.size == 2) debugAddress(1) else "none"
      }

      //If any port ranges are defined expand them into individual ports
      val portRangeChar = "-"
      val (needsExpansion, noExpansion) = serviceKeys.get(portsKey).asInstanceOf[java.util.ArrayList[String]].asScala.partition(_.contains(portRangeChar))
      val expandedPorts: Seq[String] = needsExpansion.flatMap { p =>
        val portParts = p.replaceFirst("^0:", "").split(':')
        val portSplitL = portParts(0).split(portRangeChar)
        val (rangeStartL, rangeEndL) = (portSplitL(0), portSplitL(1))
        val startL = rangeStartL.toInt
        val endL = rangeEndL.toInt
        val rangeL = endL - startL

        if (portParts.length == 1) {
          for (i <- 0 to rangeL)
            yield s"${startL + i}"
        } else {
          val portSplitR = portParts(1).split(portRangeChar)
          val (rangeStartR, rangeEndR) = (portSplitR(0), portSplitR(1))
          val startR = rangeStartR.toInt
          val endR = rangeEndR.toInt
          val rangeR = endR - startR

          if (rangeL != rangeR)
            throw new IllegalStateException(s"Invalid port range mapping specified for $p")

          for (i <- 0 to rangeR)
            yield s"${startL + i}:${startR + i}"
        }
      }

      //update ports to be the expanded version
      val list = new java.util.ArrayList[String](expandedPorts ++ noExpansion)
      serviceKeys.put(portsKey, list)

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

  def readComposeFile(composePath: String, variables: Vector[(String, String)] = Vector.empty): yamlData = {
    val yamlString = fromFile(composePath).getLines().mkString("\n")
    val yamlUpdated = processVariableSubstitution(yamlString, variables)

    new Yaml().load(yamlUpdated).asInstanceOf[java.util.Map[String, java.util.LinkedHashMap[String, Any]]].asScala.toMap
  }

  /**
   * Substitute all docker-compose variables in the YAML file.  This is traditionally done by docker-compose itself,
   * but is being performed by the plugin to support other functionality.
   * @param yamlString Stringified docker-compose file.
   * @param variables Substitution variables.
   * @return An updated stringified docker-compile file.
   */
  def processVariableSubstitution(yamlString: String, variables: Vector[(String, String)]) =
    variables.foldLeft(yamlString) {
      case (y, (key, value)) => y.replaceAll("\\$\\{" + key + "\\}", value)
    }

  def deleteComposeFile(composePath: String): Boolean = {
    Try(new File(composePath).delete()) match {
      case Success(i) => true
      case Failure(t) => false
    }
  }

  /**
   * Saves the supplied Docker Compose Yaml data to a temporary file
   *
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
