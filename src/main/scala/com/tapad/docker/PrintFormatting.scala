package com.tapad.docker

import sbt._
import scala.Console._
import scala.collection.Iterable
import com.tapad.docker.DockerComposeKeys._

trait PrintFormatting extends SettingsHelper {
  // Allows for standard print statements to be inspected for test purposes
  def print(s: String) = println(s)

  def printBold(input: String, suppressColor: Boolean): Unit = {
    if (suppressColor) {
      print(input)
    } else {
      print(BOLD + input + RESET)
    }
  }

  def printWarning(input: String, suppressColor: Boolean): Unit = {
    if (suppressColor) {
      print(input)
    } else {
      print(YELLOW + input + RESET)
    }
  }

  def printSuccess(input: String, suppressColor: Boolean): Unit = {
    if (suppressColor) {
      print(input)
    } else {
      print(GREEN + input + RESET)
    }
  }

  def printError(input: String, suppressColor: Boolean): Unit = {
    if (suppressColor) {
      print(input)
    } else {
      print(RED + input + RESET)
    }
  }

  def printTable(implicit state: State, rows: Iterable[OutputTableRow]): Unit = {
    val tableHeader = List(
      "Service",
      "Host:Port",
      "Tag Version",
      "Image Source",
      "Container Port",
      "Container Id",
      "IsDebug")
    val sortedTableEntries = rows
      .toList
      .sorted
    val outputTable = OutputTable(tableHeader :: sortedTableEntries.map(_.toStringList))

    printSuccess(outputTable.toString, getSetting(suppressColorFormatting))
  }

  def printMappedPortInformation(implicit state: State, instance: RunningInstanceInfo, composeVersion: Version): Unit = {
    val suppressColor = getSetting(suppressColorFormatting)
    printBold(s"\nThe following endpoints are available for your local instance: ${instance.instanceName}", suppressColor)
    printTable(state, getTableOutputList(instance.servicesInfo))

    print("Instance commands:")

    print(s"1) To stop instance from sbt run:")
    printSuccess(s"   dockerComposeStop ${instance.instanceName}", suppressColor)

    print(s"2) To open a command shell from bash run:")
    printSuccess(s"   docker exec -it <Container Id> bash", suppressColor)

    print(s"3) To view log files from bash run:")

    val tailFlag = if (composeVersion.major > 1 || (composeVersion.major == 1 && composeVersion.minor >= 7)) "-f" else ""
    printSuccess(s"   docker-compose -p ${instance.instanceName} -f ${instance.composeFilePath} logs $tailFlag", suppressColor)

    print(s"4) To execute test cases against instance from sbt run:")
    printSuccess(s"   dockerComposeTest ${instance.instanceName}", suppressColor)
  }

  def getTableOutputList(servicesInfo: Iterable[ServiceInfo]): Iterable[OutputTableRow] = {
    servicesInfo.flatMap { service =>
      if (service.ports.isEmpty) {
        List(
          OutputTableRow(
            service.serviceName,
            service.containerHost + ":" + "<none>",
            service.versionTag, service.imageSource,
            "<none>",
            service.containerId,
            false))
      } else {
        service.ports.map { port =>
          OutputTableRow(
            service.serviceName,
            service.containerHost + ":" + port.hostPort,
            service.versionTag,
            service.imageSource,
            port.containerPort,
            service.containerId,
            port.isDebug)
        }
      }
    }
  }
}
