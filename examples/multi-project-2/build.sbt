import sbt.Keys._

name := "multi-project-2"

version := "1.0.0"

scalaVersion := "2.11.1"

val libraries = Seq("org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalaj" %% "scalaj-http" % "2.2.1" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test"
)

val dockerAppPath = "/app/"

lazy val core = project.
  enablePlugins(sbtdocker.DockerPlugin, DockerComposePlugin).
  settings(
    composeServiceName := "core",
    dockerImageCreationTask := docker.value,
    libraryDependencies ++=libraries,
    dockerfile in docker := {
      new Dockerfile {
        val mainClassString = (mainClass in Compile).value.get
        val classpath = (fullClasspath in Compile).value
        from("java")
        add(classpath.files, dockerAppPath)
        entryPoint("java", "-cp", s"$dockerAppPath:$dockerAppPath/*", s"$mainClassString")
      }
    },
    imageNames in docker := Seq(ImageName(
    repository = name.value.toLowerCase,
    tag = Some("latest"))
    )
  )

lazy val performance = project.
  enablePlugins(DockerComposePlugin).
  settings(
    composeNoBuild := true,
    libraryDependencies ++= libraries
  )