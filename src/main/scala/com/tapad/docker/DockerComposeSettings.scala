package com.tapad.docker

import sbt._
import sbt.Keys._
import com.tapad.docker.DockerComposeKeys._
import com.tapad.docker.DockerComposePlugin._

object DockerComposeSettings extends DockerComposeSettingsLocal

trait DockerComposeSettingsLocal {
  lazy val baseDockerComposeSettings = Seq(
    // Attempt to read the compose file from the resources folder followed by a docker folder off the base directory of the project followed by the root directory
    composeFile := {
      val dockerFileName = "docker-compose.yml"
      val dockerFileInResources = (resourceDirectory in Compile).value / dockerFileName toString ()
      val dockerFileInDir = s"${baseDirectory.value.absolutePath}/docker/$dockerFileName"
      if (new File(dockerFileInResources).exists)
        dockerFileInResources
      else if (new File(dockerFileInDir).exists)
        dockerFileInDir
      else
        s"${baseDirectory.value.absolutePath}/$dockerFileName"
    },
    // By default set the Compose service name to be that of the sbt Project Name
    composeServiceName := name.value.toLowerCase,
    composeNoBuild := false,
    composeRemoveContainersOnShutdown := true,
    composeContainerStartTimeoutSeconds := 500,
    dockerMachineName := "default",
    commands ++= Seq(dockerComposeUpCommand, dockerComposeStopCommand, dockerComposeInstancesCommand)
  )
}