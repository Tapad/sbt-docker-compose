name := "basic"

version := "1.0.0"

scalaVersion := "2.10.6"

enablePlugins(JavaAppPackaging, DockerComposePlugin)

dockerImageCreationTask := (publishLocal in Docker).value

variablesForSubstitutionTask := {
  val configDataPath = (fullClasspath in Compile).value
  println(s"Get config data from $configDataPath and create Map to assign to variablesForSubstitution")
  Map("SOURCE_PORT" -> "5555")
}