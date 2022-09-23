package com.tapad.docker

import com.tapad.docker.DockerComposeKeys._

trait ComposeTestRunner extends SettingsHelper with PrintFormatting {
  val testDebugPortArg = "-debug"
  val testTagOverride = "-tags"

  /**
   * Compiles and binPackages latest test code
   *
   * @param state The sbt state
   */
  def binPackageTests(implicit state: State): Unit = {
    val extracted = Project.extract(state)
    state.globalLogging.full.info(s"Compiling and Packaging test cases using ${testCasesPackageTask.key.label} ...")
    try {
      extracted.runTask(testCasesPackageTask, state)
    } catch {
      case e: Exception =>
        throw TestCodeCompilationException(e.getMessage)
    }
  }

  /**
   * Gets a classpath representing all managed and unmanaged dependencies in the Test and Compile Scope for this sbt project.
   *
   * @param state The sbt state
   * @return The full set of classpath entries used by Test and Compile
   */
  def getTestDependenciesClassPath(implicit state: State): String = {
    val extracted = Project.extract(state)
    val (_, testClassPath) = extracted.runTask(testDependenciesClasspath, state)

    testClassPath
  }

  /**
   * Gets extra key value pairs to pass to ScalaTest in the configMap.
   *
   * @param state The sbt state
   * @return A Map[String,String] of variables to pass into the ScalaTest Runner ConfigMap
   */
  def runTestExecutionExtraConfigTask(state: State): Map[String, String] = {
    val extracted = Project.extract(state)
    val (_, value) = extracted.runTask(testExecutionExtraConfigTask, state)
    value
  }

  /**
   * Build up a set of parameters to pass to ScalaTest as a configMap.
   * Generates the list of ScalaTest Tests to execute.
   * Compiles and binPackages the latest Test code.
   * Starts a test pass using the ScalaTest Runner
   * Note: For this to work properly the version of the Scala executable on your path needs to of the same version
   * that the ScalaTest Jar was compiled with. For example,  if you are using ScalaTest 2.10.X Scala must be of
   * version 2.10.X.
   *
   * @param state    The sbt state
   * @param args     The command line arguments
   * @param instance The running Docker Compose instance to test against
   */
  def runTestPass(implicit state: State, args: Seq[String], instance: Option[RunningInstanceInfo]): State = {
    runInContainer("ScalaTest", ExecuteInput.ScalaTest)
  }

  /**
   * Build up a set of parameters to pass as System Properties that can be accessed from Specs2
   * Compiles and binPackages the latest Test code.
   * Starts a test pass using the Specs2 Files Runner
   *
   * @param state    The sbt state
   * @param args     The command line arguments
   * @param instance The running Docker Compose instance to test against
   */
  def runTestPassSpecs2(implicit state: State, args: Seq[String], instance: Option[RunningInstanceInfo]): State = {
    runInContainer("Specs2", ExecuteInput.Specs2)
  }

  /**
   * Build up a set of parameters to pass as System Properties that can be accessed from Cucumber (Cukes)
   * Compiles and binPackages the latest Test code.
   * Starts a test pass using the Cucumber Runner
   *
   * @param state    The sbt state
   * @param args     The command line arguments
   * @param instance The running Docker Compose instance to test against
   */
  def runTestPassCucumber(implicit state: State, args: Seq[String], instance: Option[RunningInstanceInfo]): State = {
    runInContainer("Cucumber", ExecuteInput.Cucumber)
  }

  /**
   * Build up a set of parameters to pass as System Properties that can be accessed from ZIO test
   * Compiles and binPackages the latest Test code.
   * Starts a test pass using the Zio Runner
   *
   * @param state    The sbt state
   * @param args     The command line arguments
   * @param instance The running Docker Compose instance to test against
   */
  def runTestPassZio(implicit state: State, args: Seq[String], instance: Option[RunningInstanceInfo]): State = {
    runInContainer("Zio", ExecuteInput.Zio)
  }

  protected def runInContainer(testDesc: String, run: ExecuteInput.Invoke)(implicit state: State, args: Seq[String], instance: Option[RunningInstanceInfo]): State = {

    val extraTestParams = runTestExecutionExtraConfigTask(state).map { case (k, v) => s"-D$k=$v" }

    binPackageTests

    // Looks for the <-debug:port> argument and will suspend test case execution until a debugger is attached
    val debugSettings: String = getArgValue(testDebugPortArg, args) match {
      case Some(port) => s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port"
      case None => ""
    }

    val suppressColor = getSetting(suppressColorFormatting)
    val testParamsList: Seq[String] = {
      //Build the list of Docker Compose connection endpoints to pass as System Properties
      //format: <-Dservice:containerPort=host:hostPort>
      val testParams = instance match {
        case Some(inst) => inst.servicesInfo.flatMap(service =>
          service.ports.map(port =>
            s"-D${service.serviceName}:${port.containerPort}=${service.containerHost}:${port.hostPort}")
            :+ s"-D${service.serviceName}:containerId=${service.containerId}").mkString(" ")
        case None => ""
      }
      testParams.split(" ").toSeq ++ extraTestParams
    }

    val testDependencies: String = getTestDependenciesClassPath
    val testInput = new ExecuteInput(this, testDependencies, testParamsList, debugSettings)

    if (run.isDefinedAt(testInput)) {
      val testRunnerCommand = run(testInput)

      if (Process(testRunnerCommand).! == 0) state
      else state.fail
    } else {
      printBold(s"Cannot find a $testDesc Jar dependency. Please make sure it is added to your sbt projects libraryDependencies.", suppressColor)
      state
    }
  }
}
