package com.tapad.docker

import sbt._
import com.tapad.docker.DockerComposeKeys._

trait DockerCommands {
  def dockerComposeUp(instanceName: String, composePath: String): Int = {
    s"docker-compose -p $instanceName -f $composePath up -d".!
  }

  def dockerComposeDown(instanceName: String, composePath: String): Int = {
    s"docker-compose -p $instanceName -f $composePath down".!
  }

  def dockerComposeStopInstance(instanceName: String, composePath: String): Unit = {
    s"docker-compose -p $instanceName -f $composePath stop".!
  }

  def dockerComposeRemoveContainers(instanceName: String, composePath: String): Unit = {
    s"docker-compose -p $instanceName -f $composePath rm -v -f".!
  }

  def getDockerComposeVersion: Version = {
    val version = "docker-compose version --short".!!
    Version(version)
  }

  def dockerPull(imageName: String): Unit = {
    s"docker pull $imageName".!
  }

  def dockerMachineIp(machineName: String): String = {
    s"docker-machine ip $machineName".!!.trim
  }

  def getDockerContainerId(instanceName: String, serviceName: String): String = {
    //Docker replaces '/' with '_' in the identifier string so search for replaced version
    s"""docker ps --all --filter=name=${instanceName.replace('/', '_')}_${serviceName}_ --format=\"{{.ID}}\"""".!!.trim().replaceAll("\"", "")
  }

  def getDockerContainerInfo(containerId: String): String = {
    s"docker inspect --type=container $containerId".!!
  }

  def dockerRemoveImage(imageName: String): Unit = {
    s"docker rmi $imageName".!!
  }

  def dockerRemoveNetwork(networkName: String, dockerMachineName: String): Unit = {
    s"docker network rm ${networkName}_$dockerMachineName".!
  }

  def dockerTagImage(currentImageName: String, newImageName: String): Unit = {
    s"docker tag $currentImageName $newImageName".!!
  }

  def dockerPushImage(imageName: String): Unit = {
    s"docker push $imageName".!
  }

  def dockerRun(command: String): Unit = {
    s"docker run $command".!
  }

  def getDockerPortMappings(containerId: String): String = {
    s"docker port $containerId".!!
  }

  def isDockerForMacEnvironment: Boolean = {
    val info = "docker info".!!
    info.contains("Operating System: Alpine Linux") && info.matches("(?s).*Kernel Version:.*-moby.*")
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
