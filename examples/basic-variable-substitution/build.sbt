name := "basic"

version := "1.0.0"

scalaVersion := "2.12.8"

enablePlugins(JavaAppPackaging, DockerComposePlugin)

dockerImageCreationTask := (publishLocal in Docker).value

variablesForSubstitution := Map("SOURCE_PORT" -> "5555")
