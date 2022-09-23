package com.tapad.docker

import sbt._
import scala.sys.process.Process
import com.tapad.docker.DockerComposeKeys._

trait DockerCommands {
  def dockerComposeUp(instanceName: String, composePath: String): Int = {
    Process(s"docker-compose -p $instanceName -f $composePath up -d").!
  }

  def dockerComposeStopInstance(instanceName: String, composePath: String): Unit = {
    Process(s"docker-compose -p $instanceName -f $composePath stop").!
  }

  def dockerComposeRemoveContainers(instanceName: String, composePath: String): Unit = {
    Process(s"docker-compose -p $instanceName -f $composePath rm -v -f").!
  }

  def dockerNetworkExists(instanceName: String, networkName: String): Boolean = {
    //Docker replaces '/' with '_' in the identifier string so search for replaced version
    //Use '-q' instead of '--format' as format was only introduced in Docker v1.13.0-rc1
    Process(s"docker network ls -q --filter=name=${instanceName.replace('/', '_')}_$networkName").!!.trim().nonEmpty
  }

  def dockerVolumeExists(instanceName: String, volumeName: String): Boolean = {
    //Docker replaces '/' with '_' in the identifier string so search for replaced version
    Process(s"docker volume ls -q --filter=name=${instanceName.replace('/', '_')}_$volumeName").!!.trim().nonEmpty
  }

  def getDockerComposeVersion: Version = {
    val version = Process("docker-compose version --short").!!
    Version(version)
  }

  def dockerPull(imageName: String): Unit = {
    Process(s"docker pull $imageName").!
  }

  def dockerMachineIp(machineName: String): String = {
    Process(s"docker-machine ip $machineName").!!.trim
  }

  def getDockerContainerId(instanceName: String, serviceName: String): String = {
    //Docker replaces '/' with '_' in the identifier string so search for replaced version
    Process(s"""docker ps --all --filter=name=${instanceName.replace('/', '-')}-${serviceName}- --format=\"{{.ID}}\"""").!!.trim().replaceAll("\"", "")
  }

  def getDockerContainerInfo(containerId: String): String = {
    Process(s"docker inspect --type=container $containerId").!!
  }

  def dockerRemoveImage(imageName: String): Unit = {
    Process(s"docker rmi $imageName").!!
  }

  def dockerRemoveNetwork(instanceName: String, networkName: String): Unit = {
    Process(s"docker network rm ${instanceName}_$networkName").!
  }

  def dockerRemoveVolume(instanceName: String, volumeName: String): Unit = {
    Process(s"docker volume rm ${instanceName}_$volumeName").!
  }

  def dockerTagImage(currentImageName: String, newImageName: String): Unit = {
    Process(s"docker tag $currentImageName $newImageName").!!
  }

  def dockerPushImage(imageName: String): Unit = {
    Process(s"docker push $imageName").!
  }

  def dockerRun(command: String): Unit = {
    Process(s"docker run $command").!
  }

  def getDockerPortMappings(containerId: String): String = {
    Process(s"docker port $containerId").!!
  }

  def isDockerForMacEnvironment: Boolean = {
    val info = Process("docker info").!!
    info.contains("Operating System: Docker for Mac") ||
      info.contains("Operating System: Docker Desktop") ||
      (info.contains("Operating System: Alpine Linux") && info.matches("(?s).*Kernel Version:.*-moby.*"))
  }

  /**
   * If running on Boot2Docker environment on OSX use the machine IP else use the container host
   * @return True if Boot2Docker, Otherwise False
   */
  def isBoot2DockerEnvironment: Boolean = sys.env.get("DOCKER_MACHINE_NAME").isDefined

  /**
   * Builds a docker image for an sbt project using the user defined task.
   * @param state The sbt state
   */
  def buildDockerImageTask(state: State): Unit = {
    val extracted = Project.extract(state)
    extracted.runTask(dockerImageCreationTask, state)
  }

  /**
   * Gets variables to use for docker-compose file substitution
   * @param state The sbt state
   */
  def runVariablesForSubstitutionTask(state: State): Vector[(String, String)] = {
    val extracted = Project.extract(state)
    val (_, value) = extracted.runTask(variablesForSubstitutionTask, state)
    value.toVector
  }
}
