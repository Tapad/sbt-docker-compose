package com.tapad.docker

import java.io._

object DockerComposeKeys extends DockerComposeKeysLocal

trait DockerComposeKeysLocal {
  val composeContainerPauseBeforeTestSeconds = settingKey[Int]("Delay between containers start and test execution, seconds. Default is 0 seconds - no delay")
  val composeFile = settingKey[String]("Specify the full path to the Compose File to use to create your test instance. It defaults to docker-compose.yml in your resources folder.")
  val composeServiceName = settingKey[String]("The name of the service in the Docker Compose file being tested. This setting prevents the service image from being pull down from the Docker Registry. This defaults to the Project name.")
  val composeServiceVersionTask = taskKey[String]("The version to tag locally built images with in the docker-compose file. This defaults to the 'version' SettingKey.")
  val composeNoBuild = settingKey[Boolean]("True if a Docker Compose file is to be started without building any images and only using ones that already exist in the Docker Registry. This defaults to False.")
  val composeRemoveContainersOnShutdown = settingKey[Boolean]("True if a Docker Compose should remove containers when shutting down the compose instance. This defaults to True.")
  val composeRemoveNetworkOnShutdown = settingKey[Boolean]("True if a Docker Compose should remove the network it created when shutting down the compose instance. This defaults to True.")
  val composeRemoveTempFileOnShutdown = settingKey[Boolean]("True if a Docker Compose should remove the post Custom Tag processed Compose File on shutdown. This defaults to True.")
  val composeContainerStartTimeoutSeconds = settingKey[Int]("The amount of time in seconds to wait for the containers in a Docker Compose instance to start. Defaults to 500 seconds.")
  val dockerMachineName = settingKey[String]("If running on OSX the name of the Docker Machine Virtual machine being used. If not overridden it is set to 'default'")
  val dockerImageCreationTask = taskKey[Any]("The sbt task used to create a Docker image. For sbt-docker this should be set to 'docker.value' for the sbt-native-packager this should be set to '(publishLocal in Docker).value'.")
  val suppressColorFormatting = settingKey[Boolean]("True to suppress all color formatting in the output from the plugin. This defaults to the value of the 'sbt.log.noformat' property.")
  val testTagsToExecute = settingKey[String]("Set of ScalaTest Tags to execute when dockerComposeTest is run. Separate multiple tags by a comma. It defaults to executing all tests.")
  val testExecutionExtraConfigTask = taskKey[Map[String, String]]("Additional ScalaTest Runner configuration to pass into the ConfigMap.")
  val testExecutionArgs = settingKey[String]("Additional ScalaTest Runner argument options to pass into the test runner. For example, this can be used for the generation of test reports.")
  val testCasesJar = settingKey[String]("The path to the Jar file containing the tests to execute. This defaults to the Jar file with the tests from the current sbt project.")
  val testCasesPackageTask = taskKey[File]("The sbt TaskKey to package the test cases used when running 'dockerComposeTest'. This defaults to the 'packageBin' task in the 'Test' Scope.")
  val testDependenciesClasspath = taskKey[String]("The path to all managed and unmanaged Test and Compile dependencies. This path needs to include the ScalaTest Jar for the tests to execute. This defaults to all managedClasspath and unmanagedClasspath in the Test and fullClasspath in the Compile Scope.")
  val testPassUseSpecs2 = settingKey[Boolean]("True if Specs2 is to be used to execute the test pass. This defaults to False and ScalaTest is used.")
  val testPassUseCucumber = settingKey[Boolean]("True if Cucumber is to be used to execute the test pass. This defaults to False and ScalaTest is used.")
  val testPassUseZio = settingKey[Boolean]("True if Zio is to be used to execute the test pass. This defaults to False and ScalaTest is used.")
  val runningInstances = AttributeKey[List[RunningInstanceInfo]]("runningInstances", "For Internal Use: Contains information on the set of running Docker Compose instances.")
  val variablesForSubstitution = settingKey[Map[String, String]]("A Map[String,String] of variables to substitute in your docker-compose file. These are substituted by the plugin and not using environment variables.")
  val variablesForSubstitutionTask = taskKey[Map[String, String]]("An sbt task that returns a Map[String,String] of variables to substitute in your docker-compose file. These are substituted by the plugin and not using environment variables.")
}
