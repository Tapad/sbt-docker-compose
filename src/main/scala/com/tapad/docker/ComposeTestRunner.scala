package com.tapad.docker

import com.tapad.docker.DockerComposeKeys._
import sbt._
import sbt.Project

trait ComposeTestRunner extends SettingsHelper with PrintFormatting {

  /**
   * Compiles and binPackages latest test code
   * @param state The sbt state
   */
  def binPackageTests(implicit state: State): Unit = {
    val extracted = Project.extract(state)
    extracted.runTask(sbt.Keys.packageBin in Test, state)
  }

  /**
   * Gets a classpath representing all managed and unmanaged dependencies in the Test Scope for this sbt project.
   * @param state The sbt state
   * @return The full set of classpath entries used by Tet
   */
  def getTestDependenciesClassPath(implicit state: State): String = {
    val extracted = Project.extract(state)
    val (_, testClassPath) = extracted.runTask(testDependenciesClasspath, state)

    testClassPath
  }

  /**
   * Build up a set of parameters to pass to ScalaTest as a configMap.
   * Generates the list of ScalaTest Tests to execute.
   * Compiles and binPackages the latest Test code.
   * Starts a test pass using the ScalaTest Runner
   * Note: For this to work properly the version of the Scala executable on your path needs to of the same version
   * that the ScalaTest Jar was compiled with. For example,  if you are using ScalaTest 2.10.X Scala must be of
   * version 2.10.X.
   * @param state The sbt state
   * @param instance The running Docker Compose instnace to test against
   */
  def runTestPass(implicit state: State, instance: Option[RunningInstanceInfo]): Unit = {
    //Build the list of Docker Compose connection endpoints to pass as a ConfigMap to the ScalaTest Runner
    //format: <-Dservice:containerPort=host:hostPort>
    val testParams = instance match {
      case Some(inst) => inst.servicesInfo.flatMap(service =>
        service.ports.map(port =>
          s"-D${service.serviceName}:${port.containerPort}=${service.containerHost}:${port.hostPort}")) mkString " "
      case None => ""
    }

    val testTags = getSetting(testTagsToExecute).split(',').filter(_.nonEmpty).map(tag => s"-n $tag") mkString " "

    print("Compiling and Packaging test cases...")
    binPackageTests

    val testDependencies = getTestDependenciesClassPath
    if (testDependencies.contains("org.scalatest")) {
      s"scala -cp $testDependencies org.scalatest.tools.Runner -o -R ${getSetting(testCasesJar)} $testTags $testParams".!
    } else {
      printBold("Cannot find a ScalaTest Jar dependency. Please make sure it is added to your sbt projects " +
        "libraryDependencies.")
    }
  }
}