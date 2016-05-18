package com.tapad.docker

import sbt._
import scala.Console._
import scala.collection.Iterable

trait PrintFormatting {
  // Allows for standard print statements to be inspected for test purposes
  def print(s: String) = println(s)

  def printBold(input: String): Unit = {
    print(BOLD + input + RESET)
  }

  def printWarning(input: String): Unit = {
    print(YELLOW + input + RESET)
  }

  def printSuccess(input: String): Unit = {
    print(GREEN + input + RESET)
  }

  def printError(input: String): Unit = {
    print(RED + input + RESET)
  }

  def printMappedPortInformation(state: State, instance: RunningInstanceInfo, composeVersion: Version): Unit = {
    printBold(s"\nThe following endpoints are available for your local instance: ${instance.instanceName}")

    val tableEntries = getTableOutputList(instance.servicesInfo)

    printSuccess(OutputTable(List("Service", "Host:Port", "Tag Version", "Image Source", "Container Port", "Container Id", "IsDebug") :: tableEntries.toList sortBy (_.head)).toString)

    print("Instance commands:")

    print(s"1) To stop instance from sbt run:")
    printSuccess(s"   dockerComposeStop ${instance.instanceName}")

    print(s"2) To open a command shell from bash run:")
    printSuccess(s"   docker exec -it <Container Id> bash")

    print(s"3) To view log files from bash run:")

    val tailFlag = if (composeVersion.major > 1 || (composeVersion.major == 1 && composeVersion.minor >= 7)) "-f" else ""
    printSuccess(s"   docker-compose -p ${instance.instanceName} -f ${instance.composeFilePath} logs $tailFlag")

    print(s"4) To execute test cases against instance from sbt run:")
    printSuccess(s"   dockerComposeTest ${instance.instanceName}")
  }

  def getTableOutputList(servicesInfo: Iterable[ServiceInfo]): Iterable[List[String]] = {
    servicesInfo.flatMap { service =>
      if (service.ports.isEmpty) {
        List(List(service.serviceName, service.containerHost + ":" + "<none>", service.versionTag, service.imageSource, "<none>", service.containerId, ""))
      } else {
        service.ports.map { port =>
          List(service.serviceName, service.containerHost + ":" + port.hostPort, service.versionTag, service.imageSource, port.containerPort, service.containerId, if (port.isDebug) "YES" else "")
        }
      }
    }
  }
}
