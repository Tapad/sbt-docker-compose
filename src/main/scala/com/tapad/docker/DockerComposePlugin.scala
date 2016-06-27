package com.tapad.docker

import com.tapad.docker.DockerComposeKeys._
import net.liftweb.json._
import sbt._

import scala.Console._
import scala.collection._
import scala.concurrent.duration._
import scala.util.Try

/**
 * The exception type to be thrown when there is a format issue in the Compose file
 *
 * @param message The error message to be displayed on the console
 */
case class ComposeFileFormatException(message: String) extends Exception(message)

/**
 * Defines an internal to external port mapping for a Docker Compose service port
 *
 * @param hostPort The port that is externally exposed for access from the Docker host machine
 * @param containerPort The port that is internally exposed for access within the Docker Compose instance
 * @param isDebug True if this port for connecting to for debugging purposes, False otherwise.
 */
case class PortInfo(hostPort: String, containerPort: String, isDebug: Boolean)

/**
 * Represents a Docker Compose service entry
 *
 * @param serviceName The name of the Docker Compose service
 * @param imageName The full image name of the image for the service
 * @param imageSource An identifier representing where this image is being retrieved from. For example, "cache",
 *                    "build", or "defined". More types can be added in extended classes.
 * @param ports A collection of ports that are defined by the service
 * @param containerId The container ID of the running service
 * @param containerHost The container Host (name or IP) of the running service
 */
case class ServiceInfo(serviceName: String, imageName: String, imageSource: String, ports: List[PortInfo],
    containerId: String = "", containerHost: String = "") extends ComposeCustomTagHelpers {
  val versionTag = getTagFromImage(imageName)
}

/**
 * Represents a running Docker Compose instance
 *
 * @param instanceName The unique identifier that represents a running Docker Compose instance
 * @param composeServiceName The SBT defined settingKey for composeServiceName. This allows an instance to be associated
 *                          with an SBT project.
 * @param composeFilePath The path to the Docker Compose file used by this instance
 * @param servicesInfo The collection of ServiceInfo objects that define this instance
 * @param instanceData An optional parameter to specify additional information about the instance
 */
case class RunningInstanceInfo(instanceName: String, composeServiceName: String, composeFilePath: String,
  servicesInfo: Iterable[ServiceInfo], instanceData: Option[Any] = None)

object DockerComposePlugin extends DockerComposePluginLocal {
  override def projectSettings = DockerComposeSettings.baseDockerComposeSettings

  //Import these settings so that they can easily be configured from a projects build.sbt
  object autoImport {
    val composeFile = DockerComposeKeys.composeFile
    val composeServiceName = DockerComposeKeys.composeServiceName
    val composeNoBuild = DockerComposeKeys.composeNoBuild
    val composeRemoveContainersOnShutdown = DockerComposeKeys.composeRemoveContainersOnShutdown
    val composeRemoveNetworkOnShutdown = DockerComposeKeys.composeRemoveNetworkOnShutdown
    val composeRemoveTempFileOnShutdown = DockerComposeKeys.composeRemoveTempFileOnShutdown
    val composeContainerStartTimeoutSeconds = DockerComposeKeys.composeContainerStartTimeoutSeconds
    val dockerMachineName = DockerComposeKeys.dockerMachineName
    val dockerImageCreationTask = DockerComposeKeys.dockerImageCreationTask
    val testDependenciesClasspath = DockerComposeKeys.testDependenciesClasspath
    val testTagsToExecute = DockerComposeKeys.testTagsToExecute
    val testCasesJar = DockerComposeKeys.testCasesJar
    val scalaTestJar = DockerComposeKeys.testDependenciesClasspath
    val variablesForSubstitution = DockerComposeKeys.variablesForSubstitution
  }
}

/**
 * SBT Plug-in that allows for local Docker Compose instances to be managed directly from SBT.
 * This class can be extended to manage Docker Compose instances in non-local environments such as Mesos or AWS.
 */
class DockerComposePluginLocal extends AutoPlugin with ComposeFile with DockerCommands with ComposeTestRunner with ComposeInstancePersistence {
  //Command line arguments
  val skipPullArg = "skipPull"
  val skipBuildArg = "skipBuild"
  lazy val dockerComposeVersion = getDockerComposeVersion

  lazy val dockerComposeUpCommand = Command.args("dockerComposeUp", ("dockerComposeUp", "Starts a local Docker Compose instance."),
    s"Supply '$skipPullArg' as a parameter to use local images instead of pulling the latest from the Docker Registry. " +
      s"Supply '$skipBuildArg' as a parameter to use the current Docker image for the main project instead of building a new one." +
      s"Supply '$useStaticPortsArg' as a parameter to use static host ports instead of the Docker dynamically assigned host ports.", "") {
      (state: State, args: Seq[String]) =>
        try {
          launchInstanceWithLatestChanges(state, args)
        } catch {
          case ex: ComposeFileFormatException =>
            printError(ex.message)
            state
          case ex: IllegalStateException =>
            printError(ex.getMessage)
            state
        }
    }

  lazy val dockerComposeStopCommand = Command.args("dockerComposeStop", ("dockerComposeStop", "Stops all local Docker " +
    "Compose instances started in this sbt project."),
    "Supply the Instance Id to just stop a particular instance", "") {
      (state: State, args: Seq[String]) =>

        stopRunningInstances(state, args)
    }

  lazy val dockerComposeRestartCommand = Command.args("dockerComposeRestart", ("dockerComposeRestart", "Restarts a local Docker Compose instance."),
    "Supply the Instance Id to restart a particular instance. " +
      s"Supply '$skipPullArg' as a parameter to use local images instead of pulling the latest from the Docker Registry. " +
      s"Supply '$skipBuildArg' as a parameter to use the current Docker image for the main project instead of building a new one." +
      s"Supply '$useStaticPortsArg' as a parameter to use static host ports instead of the Docker dynamically assigned host ports.", "") {
      (state: State, args: Seq[String]) =>

        restartRunningInstance(state, args)
    }

  lazy val dockerComposeInstancesCommand = Command.args("dockerComposeInstances", ("dockerComposeInstances", "Prints a " +
    "table of information for all running Docker Compose instances."), "", "") {
    (state: State, args: Seq[String]) =>

      printDockerComposeInstances(state, args)
  }

  lazy val dockerComposeTest = Command.args("dockerComposeTest", ("dockerComposeTest", "Executes ScalaTest test " +
    "cases against a newly started Docker Compose instance."),
    s"Supply '$skipPullArg' as a parameter to use local images instead of pulling the latest from the Docker Registry." +
      s"Supply '$skipBuildArg' as a parameter to use the current Docker image for the main project instead of building a new one." +
      s"Supply '$testDebugPortArg:<port> as a parameter to cause the test execution to wait for a debugger to be attached on the specified port" +
      s"Supply '$testTagOverride:<tagName1,tagName2>' as a parameter to override the tags specified in the testTagsToExecute setting." +
      "To execute a test pass against a previously started dockerComposeUp instance just pass the instance id to the command as a parameter", "") { (state: State, args: Seq[String]) =>

      try {
        composeTestRunner(state, args)
      } catch {
        case ex: ComposeFileFormatException =>
          printError(ex.message)
          state
      }
    }

  def launchInstanceWithLatestChanges(state: State, args: Seq[String]): State = {
    val newState = getPersistedState(state)

    buildDockerImage(newState, args)

    val (startedState, _) = startDockerCompose(newState, args)

    startedState
  }

  def stopRunningInstances(state: State, args: Seq[String]): State = {
    val newState = getPersistedState(state)

    //If the caller supplied arguments then attempt to stop the instances provided
    if (args.nonEmpty) {
      stopDockerCompose(newState, args)
    } else {
      //By default if no arguments are passed stop all instances from current sbt project
      val runningInstances = getServiceRunningInstanceIds(newState)
      stopDockerCompose(newState, runningInstances)
    }
  }

  def restartRunningInstance(state: State, args: Seq[String]): State = {
    try {
      val newState1 = restartInstancePrecheck(state, args)
      val newState2 = stopRunningInstances(newState1, args)
      launchInstanceWithLatestChanges(newState2, args)
    } catch {
      case ex: IllegalArgumentException =>
        printError(ex.getMessage)
        state
      case ex: ComposeFileFormatException =>
        printError(ex.getMessage)
        state
    }
  }

  def restartInstancePrecheck(state: State, args: Seq[String]): State = {
    val newState = getPersistedState(state)
    val runningInstanceIds = getServiceRunningInstanceIds(newState)

    if (args.isEmpty) {
      if (runningInstanceIds.size > 1)
        throw new IllegalArgumentException("More than one running instance from the current sbt project was detected. " +
          "Please provide an Instance Id parameter to the dockerComposeRestart command specifying which instance to stop.")
    } else {
      val restartList = runningInstanceIds.filter(args.contains(_))
      if (restartList.isEmpty)
        throw new IllegalArgumentException("No local Docker Compose instances found to restart from current sbt project.")
    }

    newState
  }

  /**
   * startDockerCompose creates a local Docker Compose instance based on the defined compose file definition. The
   * compose instance will be randomly named so that it will not conflict with any currently running instances. It will
   * also save the name of this instance to the settings state so that it can be stopped via dockerComposeStop. After
   * the instance is started the set of connection information for the services will be printed to the console.
   *
   * @param state Current sbt setting state for the project
   * @param args Supply 'skipPull' if locally cached images should be used in the Docker Compose instance. Otherwise,
   *            images will be pulled from the Docker Registry.
   * @return The updated sbt session state along with the generated instance name
   */
  def startDockerCompose(implicit state: State, args: Seq[String]): (State, String) = {
    val composeFilePath = getSetting(composeFile)
    val variables = getSetting(variablesForSubstitution).toVector

    printBold(s"Creating Local Docker Compose Environment.")
    printBold(s"Reading Compose File: $composeFilePath")

    val composeYaml = readComposeFile(composeFilePath, variables)
    val servicesInfo = processCustomTags(state, args, composeYaml)
    val updatedComposePath = saveComposeFile(composeYaml)
    println(s"Created Compose File with Processed Custom Tags: $updatedComposePath")

    pullDockerImages(args, servicesInfo)

    //Generate random instance name so that it won't collide with other instances running and so that it can be uniquely
    //identified from the list of running containers
    val instanceName = generateInstanceName(state)

    val newState = Try {
      val ret = dockerComposeUp(instanceName, updatedComposePath)
      if (ret != 0) throw new IllegalStateException()

      val newInstance = getRunningInstanceInfo(state, instanceName, updatedComposePath, servicesInfo)

      printMappedPortInformation(state, newInstance, dockerComposeVersion)
      saveInstanceToSbtSession(state, newInstance)
    } getOrElse {
      stopLocalDockerInstance(state, instanceName, updatedComposePath)
      throw new IllegalStateException(s"Error starting Docker Compose instance. Shutting down containers...")
    }

    saveInstanceState(newState)

    (newState, instanceName)
  }

  def getRunningInstanceInfo(implicit state: State, instanceName: String, composePath: String,
    servicesInfo: Iterable[ServiceInfo]): RunningInstanceInfo = {
    val composeService = getSetting(composeServiceName).toLowerCase
    val composeStartTimeout = getSetting(composeContainerStartTimeoutSeconds)
    val dockerMachine = getSetting(dockerMachineName)

    val serviceInfo = populateServiceInfoForInstance(instanceName, dockerMachine, servicesInfo, composeStartTimeout)

    RunningInstanceInfo(instanceName, composeService, composePath, serviceInfo)
  }

  def pullDockerImages(args: Seq[String], services: Iterable[ServiceInfo]): Unit = {
    if (containsArg(skipPullArg, args)) {
      print(s"'$skipPullArg' argument supplied. Skipping Docker Repository Pull for all images. Using locally cached " +
        s"version of images.")
    } else {
      //Pull down the dependent images ignoring locally built images since we want to test the local changes and not
      // what has been already published
      printBold(s"Pulling Docker images except for locally built images and images defined as <skipPull> or <localBuild>.")

      val (skipPull, pull) = services.partition(service => service.imageSource == buildImageSource || service.imageSource == cachedImageSource)
      skipPull.foreach(service => printBold(s"Skipping Pull of image: ${service.imageName}"))
      pull.foreach(service => dockerPull(service.imageName))
    }
  }

  /**
   * stopDockerCompose stops the local Docker Compose instances that was previously started by startDockerCompose. It
   * will also by default remove the containers and volumes created by Docker Compose.
   *
   * @param state Current sbt setting state for the project.
   * @param args List of instance id's to stop.
   * @return The updated sbt session state.
   */
  def stopDockerCompose(implicit state: State, args: Seq[String]): State = {
    val updatedState = getAttribute(runningInstances) match {
      case Some(launchedInstances) =>
        val (removeList, keepList) = launchedInstances.partition(instance => args.contains(instance.instanceName))
        //Remove all of the stopped instances from the list
        removeList.foreach { instance =>
          printBold(s"Stopping and removing local Docker instance: ${instance.instanceName}")
          stopLocalDockerInstance(state, instance.instanceName, instance.composeFilePath)
        }

        if (removeList.isEmpty)
          printWarning(s"No local Docker Compose instances found to stop from current sbt project.")

        if (keepList.nonEmpty) {
          setAttribute(runningInstances, keepList)
        } else {
          removeAttribute(runningInstances)
        }
      case None =>
        print(s"No local Docker Compose instances found to stop from current sbt project.")
        state
    }

    saveInstanceState(updatedState)

    updatedState
  }

  def stopLocalDockerInstance(implicit state: State, instanceName: String, composePath: String): Unit = {
    dockerComposeStopInstance(instanceName, composePath)

    if (getSetting(composeRemoveContainersOnShutdown)) {
      dockerComposeRemoveContainers(instanceName, composePath)
    }

    if (getSetting(composeRemoveNetworkOnShutdown)) {
      // If the compose file being used is a version that creates a new network on startup then remove that network on
      // shutdown
      if (new File(composePath).exists()) {
        val composeYaml = readComposeFile(composePath)
        if (getComposeVersion(composeYaml) >= 2) {
          val dockerMachine = getSetting(dockerMachineName)
          dockerRemoveNetwork(instanceName, dockerMachine)
        }
      }
    }

    //When shutting down the instance remove the tag processed compose file by default. This is an option as it can be
    //useful to have this file for debugging purposes.
    if (getSetting(composeRemoveTempFileOnShutdown)) {
      deleteComposeFile(composePath)
    }
  }

  def buildDockerImage(implicit state: State, args: Seq[String]): Unit = {
    if (!getSetting(composeNoBuild)) {
      if (containsArg(skipBuildArg, args)) {
        print(s"'$skipBuildArg' argument supplied. Using the current local Docker image instead of building a new one.")
      } else {
        printBold("Building a new Docker image.")
        buildDockerImageTask(state)
      }
    }
  }

  def populateServiceInfoForInstance(instanceName: String, dockerMachineName: String, services: Iterable[ServiceInfo],
    timeout: Int): Iterable[ServiceInfo] = {
    //For all of the defined ports in the compose file get the port information from the locally running Docker containers
    services.map { service =>
      val serviceName = service.serviceName
      val deadline = timeout.seconds.fromNow
      var containerId = ""
      do {
        print(s"Waiting for container Id to be available for service '$serviceName' time remaining: ${deadline.timeLeft.toSeconds}")
        containerId = getDockerContainerId(instanceName, serviceName)
        if (containerId.isEmpty) Thread.sleep(2000)
      } while (containerId.isEmpty && deadline.hasTimeLeft)

      if (!deadline.hasTimeLeft) {
        printError(s"Cannot determine container Id for service: $serviceName")
        throw new IllegalStateException(s"Cannot determine container Id for service: $serviceName")
      }

      print(s"$serviceName Container Id: $containerId")

      print(s"Inspecting container $containerId to get the port mappings")
      val containerInspectInfo = getDockerContainerInfo(containerId)
      val jsonInspect = parse(containerInspectInfo)
      //For each internal container port find its externally mapped host accessible port
      val portsWithHost = service.ports.map { port =>
        //If not specified assume it's a tcp port
        val portFullName = if (port.containerPort.contains("/")) port.containerPort else s"${port.containerPort}/tcp"
        val hostPort = compact(render(jsonInspect \ "NetworkSettings" \ "Ports" \ portFullName \ "HostPort")).replaceAll("\"", "")
        PortInfo(hostPort, port.containerPort, port.isDebug)
      }
      val containerHost = getContainerHost(dockerMachineName, instanceName, jsonInspect)

      service.copy(ports = portsWithHost, containerId = containerId, containerHost = containerHost)
    }
  }

  def composeTestRunner(implicit state: State, args: Seq[String]): State = {
    val newState = getPersistedState(state)

    val requiresShutdown = getMatchingRunningInstance(newState, args).isEmpty
    val (finalState, instance) = getTestPassInstance(newState, args)

    runTestPass(finalState, args, instance)

    if (requiresShutdown)
      stopDockerCompose(finalState, Seq(instance.get.instanceName))
    else
      finalState
  }

  def getTestPassInstance(state: State, args: Seq[String]): (State, Option[RunningInstanceInfo]) = {
    //Check to see if a running instance was passed in which case kick off a test pass against it without starting and
    //stopping a new instance
    getMatchingRunningInstance(state, args) match {
      case Some(runningInstance) =>
        printBold(s"Starting Test Pass against the running local Docker Compose instance: ${runningInstance.instanceName}")
        (state, getMatchingRunningInstance(state, Seq(runningInstance.instanceName)))
      case None =>
        printBold(s"Starting Test Pass against a new local Docker Compose instance.")
        buildDockerImage(state, args)
        //Get the set of decorated endpoints and the randomly generated name of the project
        val (newState2, instanceId) = startDockerCompose(state, args)

        (newState2, getMatchingRunningInstance(newState2, Seq(instanceId)))
    }
  }

  def printDockerComposeInstances(state: State, args: Seq[String]): State = {
    val newState = getPersistedState(state)

    getAttribute(runningInstances)(newState) match {
      case Some(launchedInstances) =>
        //Print all of the connection information for each running instance
        launchedInstances.foreach(printMappedPortInformation(newState, _, dockerComposeVersion))
      case None =>
        print("There are no currently running Docker Compose instances detected.")
    }

    newState
  }

  /**
   * Determines the host connection string for a container
   *
   * @param dockerMachineName If on OSX the name of the Docker Machine being used
   * @param instanceName The name of the Docker Compose instance
   * @param json The "docker inspect" json output for a container
   * @return The IP or host that can be used to access a container
   */
  def getContainerHost(dockerMachineName: String, instanceName: String, json: JValue): String = {
    if (isBoot2DockerEnvironment) {
      print("OSX boot2docker environment detected. Using the docker-machine IP for the container.")
      dockerMachineIp(dockerMachineName)
    } else {
      print("Non-OSX environment detected. Using the host from the container.")

      //If a custom network for the Compose Container has been created (as is done with Compose 2.0)
      //then attempt to use that. Otherwise, use the higher-level NetworkSettings Gateway setting
      val networkHost = json \ "NetworkSettings" \ "Networks" \ s"${instanceName}_$dockerMachineName" \ "Gateway"
      networkHost match {
        case JNothing => compact(render(json \ "NetworkSettings" \ "Gateway")).replaceAll("\"", "")
        case _ => compact(render(networkHost)).replaceAll("\"", "")
      }
    }
  }

  /**
   * Generates an instance name that is unique from that of the running containers
   *
   * @return The generated instance name
   */
  def generateInstanceName(state: State): String = {
    /**
     * Recursively looks for an instance name that is unique
     *
     * @param runningIds The current list of running instance id's
     * @return The unique instance name
     */
    def generateInstanceNameRec(runningIds: Seq[String]): String = {
      val randNum = scala.util.Random.nextInt(1000000).toString
      if (runningIds.contains(randNum)) generateInstanceNameRec(runningIds) else randNum
    }

    val runningIds = getAllRunningInstanceIds(state)
    generateInstanceNameRec(runningIds)
  }
}