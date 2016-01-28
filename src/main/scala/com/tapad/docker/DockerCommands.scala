package com.tapad.docker

import sbt.Keys._
import sbt._
import sbtdocker.DockerKeys._
import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerImagePluginType._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

trait DockerCommands {
  def dockerComposeUp(instanceName: String, composePath: String): Unit = {
    s"docker-compose -p $instanceName -f $composePath up -d".!
  }

  def dockerComposeStopInstance(instanceName: String, composePath: String): Unit = {
    s"docker-compose -p $instanceName -f $composePath stop".!
  }

  def dockerComposeRemoveContainers(instanceName: String, composePath: String): Unit = {
    s"docker-compose -p $instanceName -f $composePath rm -v -f".!
  }

  def dockerPull(imageName: String): Unit = {
    s"docker pull $imageName".!
  }

  def dockerMachineIp(machineName: String): String = {
    s"docker-machine ip $machineName".!!.trim
  }

  def getDockerContainerId(instanceName: String, serviceName: String): String = {
    //Docker replaces '/' with '_' in the identifier string so search for replaced version
    s"""docker ps --filter=\"name=${instanceName.replace('/', '_')}_${serviceName}_\" --format=\"{{.ID}}\"""".!!.trim()
  }

  def getDockerContainerInfo(containerId: String): String = {
    s"docker inspect --type=container $containerId".!!
  }

  def dockerRemoveImage(imageName: String): Unit = {
    s"docker rmi $imageName".!!
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

  /**
   * If running on Boot2Docker environment on OSX use the machine IP else use the container host
   * @return True if Boot2Docker, Otherwise False
   */
  def isBoot2DockerEnvironment: Boolean = {
    //
    sys.env.get("DOCKER_MACHINE_NAME") match {
      case Some(_) => true
      case None => false
    }
  }

  /**
   * Builds a docker image for an sbt project using either the sbt-docker or sbt-native-packager plugins
   * Override this method to support additional Docker image creation plugins
   * @param state The sbt state
   */
  def buildDockerImageTask(state: State): Unit = {
    val extracted = Project.extract(state)
    extracted.get(dockerImageCreationPlugin) match {
      case SbtDocker => extracted.runTask(docker, state)
      case NativePackager => extracted.runTask(publishLocal in Docker, state)
    }
  }
}
