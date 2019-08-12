package com.tapad.docker

import com.tapad.docker.DockerComposeKeys.{ suppressColorFormatting, testCasesJar, testTagsToExecute }
import sbt.State

import scala.collection.Seq

/**
 * Represents the settings/input given to produce a test command-line.
 */
case class ExecuteInput(
  runner: ComposeTestRunner,
  testDependencyClasspath: String,
  testParamsList: Seq[String],
  debugSettings: String)(implicit
  val state: State,
  val args: Seq[String],
  val instance: Option[RunningInstanceInfo]) {
  def matches(regex: String) = testDependencyClasspath.matches(regex)

  def testArgs = runner.getSetting(DockerComposeKeys.testExecutionArgs).split(" ").toSeq

  def suppressColor = runner.getSetting(suppressColorFormatting)

  def formattedClasspath = {
    testDependencyClasspath.split("[;:,]", -1).mkString("\n")
  }

  override def toString = {
    s"""Docker Compose Test Input:
       |testParamsList: $testParamsList
       |debugSettings: $debugSettings
       |testClasspath: $formattedClasspath
     """.stripMargin
  }
}

object ExecuteInput {
  type TestRunnerCommand = Seq[String]

  /**
   * Given an ExecuteInput, produce an option command-line used to execute some tests
   */
  type Invoke = PartialFunction[ExecuteInput, TestRunnerCommand]

  /**
   * A function which will execute ScalaTests given an [[ExecuteInput]]
   */
  val ScalaTest: PartialFunction[ExecuteInput, TestRunnerCommand] = {
    case input: ExecuteInput if input.matches(".*org.scalatest.*") =>

      val outputArg = "-o"
      val testTags = (input.runner.getArgValue(input.runner.testTagOverride, input.args) match {
        case Some(tag) => tag
        case None => input.runner.getSetting(testTagsToExecute)(input.state)
      }).split(',').filter(_.nonEmpty).map(tag => s"-n $tag").mkString(" ")

      val noColorOption = if (input.suppressColor) "W" else ""

      //If testArgs contains '-o' values then parse them out to combine with the existing '-o' setting
      val (testArgsOutput, testArgs) = input.testArgs.partition(_.startsWith(outputArg))
      val outputFormattingArgs = testArgsOutput.map(_.replace(outputArg, "")).headOption.getOrElse("")

      val jarName = input.runner.getSetting(testCasesJar)(input.state)

      val testRunnerCommand: Seq[String] = (Seq("java", input.debugSettings) ++
        input.testParamsList ++
        Seq("-cp", input.testDependencyClasspath, "org.scalatest.tools.Runner", s"$outputArg$noColorOption$outputFormattingArgs") ++
        testArgs ++
        Seq("-R", s"${jarName.replace(" ", "\\ ")}") ++
        testTags.split(" ").toSeq ++
        input.testParamsList).filter(_.nonEmpty)

      testRunnerCommand
  }

  /**
   * A function which will execute Specs2 tests given an [[ExecuteInput]]
   */
  val Specs2: PartialFunction[ExecuteInput, TestRunnerCommand] = {
    case input: ExecuteInput if input.matches(".*org.specs2.*") =>

      val noColorOption = if (input.suppressColor) "-Dspecs2.color=false" else ""

      val testParamsList = input.testParamsList
      val testRunnerCommand = (Seq("java", input.debugSettings, noColorOption) ++
        input.testParamsList ++
        Seq("-cp", input.testDependencyClasspath, "org.specs2.runner.files") ++
        input.testArgs ++
        testParamsList).filter(_.nonEmpty)

      testRunnerCommand
  }

  /**
   * A function which will execute Cucumber tests given an [[ExecuteInput]]
   *
   * @see https://cucumber.io/docs/reference/jvm#java
   */
  val Cucumber: PartialFunction[ExecuteInput, TestRunnerCommand] = {
    case input: ExecuteInput if input.matches(".*cucumber.*") =>

      val cucumberArgs = {
        val gluePath = "classpath:"
        val featurePath = "classpath:"

        val noColorOption = if (input.suppressColor) "-m" else ""
        Seq("--glue", gluePath, noColorOption, featurePath) ++ input.testArgs
      }
      val testRunnerCommand = (Seq("java", input.debugSettings) ++
        input.testParamsList ++
        Seq("-cp", input.testDependencyClasspath, "cucumber.api.cli.Main") ++
        cucumberArgs).filter(_.nonEmpty)

      testRunnerCommand
  }
}