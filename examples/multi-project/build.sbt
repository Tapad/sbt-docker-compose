name := "multi-project"

version := "1.0.0"

scalaVersion := "2.12.8"

enablePlugins(DockerComposePlugin)

docker <<= (docker in sample1, docker in sample2) map {(image, _) => image}

dockerImageCreationTask := docker.value

val dockerAppPath = "/app/"

lazy val sample1 = project.
  enablePlugins(sbtdocker.DockerPlugin, DockerComposePlugin).
  settings(
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

lazy val sample2 = project.
  enablePlugins(sbtdocker.DockerPlugin, DockerComposePlugin).
  settings(
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
