
name := "basic"

version := "1.0.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.0.1" % "it",
  "org.scalaj" %% "scalaj-http" % "2.2.1" % "it",
  "org.pegdown" % "pegdown" % "1.6.0" % "it"
)

enablePlugins(DockerPlugin, DockerComposePlugin)

Defaults.itSettings

lazy val root = project.in(file(".")).configs(IntegrationTest)

//To use 'dockerComposeTest' to run tests in the 'IntegrationTest' scope instead of the default 'Test' scope:
// 1) Package the tests that exist in the IntegrationTest scope
testCasesPackageTask := (sbt.Keys.packageBin in IntegrationTest).value
// 2) Specify the path to the IntegrationTest jar produced in Step 1
testCasesJar := artifactPath.in(IntegrationTest, packageBin).value.getAbsolutePath
// 3) Include any IntegrationTest scoped resources on the classpath if they are used in the tests
testDependenciesClasspath := {
  val fullClasspathCompile = (fullClasspath in Compile).value
  val classpathTestManaged = (managedClasspath in IntegrationTest).value
  val classpathTestUnmanaged = (unmanagedClasspath in IntegrationTest).value
  val testResources = (resources in IntegrationTest).value
  (fullClasspathCompile.files ++ classpathTestManaged.files ++ classpathTestUnmanaged.files ++ testResources).map(_.getAbsoluteFile).mkString(":")
}

//Set the image creation Task to be the one used by sbt-docker
dockerImageCreationTask := docker.value

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