package com.tapad.docker

import com.tapad.docker.DockerComposeKeys._
import sbt._
import sbt.Project

import scala.collection.Seq

trait ComposeTestRunner extends SettingsHelper with PrintFormatting {
  val testDebugPortArg = "-debug"
  val testTagOverride = "-tags"

  /**
   * Compiles and binPackages latest test code
   * @param state The sbt state
   */
  def binPackageTests(implicit state: State): Unit = {
    val extracted = Project.extract(state)
    extracted.runTask(sbt.Keys.packageBin in Test, state)
  }

  /**
   * Gets a classpath representing all managed and unmanaged dependencies in the Test and Compile Scope for this sbt project.
   * @param state The sbt state
   * @return The full set of classpath entries used by Test and Compile
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
   * @param args The command line arguments
   * @param instance The running Docker Compose instance to test against
   */
  def runTestPass(implicit state: State, args: Seq[String], instance: Option[RunningInstanceInfo]): State = {
    //Build the list of Docker Compose connection endpoints to pass as a ConfigMap to the ScalaTest Runner
    //format: <-Dservice:containerPort=host:hostPort>
    val testParams = instance match {
      case Some(inst) => inst.servicesInfo.flatMap(service =>
        service.ports.map(port =>
          s"-D${service.serviceName}:${port.containerPort}=${service.containerHost}:${port.hostPort}")
          :+ s"-D${service.serviceName}:containerId=${service.containerId}").mkString(" ")
      case None => ""
    }

    print("Compiling and Packaging test cases...")
    binPackageTests

    // Looks for the <-debug:port> argument and will suspend test case execution until a debugger is attached
    val debugSettings = getArgValue(testDebugPortArg, args) match {
      case Some(port) => s"-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port"
      case None => ""
    }

    val testTags = (getArgValue(testTagOverride, args) match {
      case Some(tag) => tag
      case None => getSetting(testTagsToExecute)
    }).split(',').filter(_.nonEmpty).map(tag => s"-n $tag").mkString(" ")

    val testDependencies = getTestDependenciesClassPath
    if (testDependencies.matches(".*org.scalatest.*")) {
      val testParamsList = testParams.split(" ").toSeq
      val testRunnerCommand = (Seq("java", debugSettings) ++
        testParamsList ++
        Seq("-cp", testDependencies, "org.scalatest.tools.Runner", "-o", "-R", s"${getSetting(testCasesJar).replace(" ", "\\ ")}") ++
        testTags.split(" ").toSeq ++
        testParamsList).filter(_.nonEmpty)
      if (testRunnerCommand.! == 0) state
      else state.fail
    } else {
      printBold("Cannot find a ScalaTest Jar dependency. Please make sure it is added to your sbt projects " +
        "libraryDependencies.")
      state
    }
  }
}
