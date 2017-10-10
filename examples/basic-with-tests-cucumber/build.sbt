

name := "basic-cucumber"

version := "1.0.0"

scalaVersion := "2.11.11"

enablePlugins(DockerPlugin, DockerComposePlugin, CucumberPlugin)

libraryDependencies ++= {
  val cucumber = List("core", "jvm", "junit").map(suffix =>
    "info.cukes" % s"cucumber-$suffix" % "1.2.5" % "test") :+ ("info.cukes" %% "cucumber-scala" % "1.2.5" % "test")

  cucumber ::: List(
    "org.scalactic" %% "scalactic" % "3.0.4" % "test",
    "org.scalatest" %% "scalatest" % "3.0.4" % ("test->*"),
    "org.pegdown" % "pegdown" % "1.6.0" % ("test->*"),
    "junit" % "junit" % "4.12" % "test"
  )
}

CucumberPlugin.glue := "classpath:"
CucumberPlugin.features += "classpath:"

//Set the image creation Task to be the one used by sbt-docker
dockerImageCreationTask := docker.value

testPassUseCucumber := true

imageNames in docker := Seq(ImageName(
  repository = name.value.toLowerCase,
  tag = Some(version.value))
)

// create a docker file with a file /inputs/example.input
dockerfile in docker := {

  val classpath: Classpath = (fullClasspath in Test).value
  sLog.value.debug(s"Classpath is ${classpath.files.mkString("\n")}\n")

  new Dockerfile {
    val dockerAppPath = "/app/"
    from("java")
    add(classpath.files, dockerAppPath)

    entryPoint("java", "-cp", s"$dockerAppPath:$dockerAppPath*", "example.CalculatorServer")
  }
}
