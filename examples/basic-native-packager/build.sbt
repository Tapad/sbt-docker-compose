name := "basic"

version := "1.0.0"

scalaVersion := "2.10.6"

enablePlugins(JavaAppPackaging, DockerComposePlugin)

dockerImageCreationTask := (publishLocal in Docker).value