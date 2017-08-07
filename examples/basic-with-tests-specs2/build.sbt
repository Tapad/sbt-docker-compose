name := "basic"

version := "1.0.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.7.3" % "test",
  "org.scalaj" %% "scalaj-http" % "2.2.1" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test"
)

enablePlugins(DockerPlugin, DockerComposePlugin)

//Set the image creation Task to be the one used by sbt-docker
dockerImageCreationTask := docker.value

testPassUseSpecs2 := true

testExecutionExtraConfigTask := Map("filesrunner.verbose" -> s"true")

dockerfile in docker := {
  new Dockerfile {
    val dockerAppPath = "/app/"
    val mainClassString = (mainClass in Compile).value.get
    val classpath = (fullClasspath in Compile).value
    from("java")
    add(classpath.files, dockerAppPath)
    entryPoint("java", "-cp", s"$dockerAppPath:$dockerAppPath/*", s"$mainClassString")
  }
}

imageNames in docker := Seq(ImageName(
  repository = name.value.toLowerCase,
  tag = Some(version.value))
)