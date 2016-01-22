sbtPlugin := true

name := "sbt-docker-compose"

organization := "com.tapad.docker"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.5"

resolvers += "Scala Tools Nexus" at "http://nexus.tapad.com:8080/nexus/content/groups/aggregate/"

libraryDependencies ++= Seq("net.liftweb" %% "lift-json" % "2.5-RC5",
  "org.yaml" % "snakeyaml" % "1.15",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test")

val tapadNexusSnapshots = "Tapad Nexus Snapshots" at "http://nexus.tapad.com:8080/nexus/content/repositories/snapshots"

val tapadNexusReleases = "Tapad Nexus Releases" at "http://nexus.tapad.com:8080/nexus/content/repositories/releases"

publishTo <<= version { version: String =>
  if (version.endsWith("SNAPSHOT") || version.endsWith("TAPAD"))
    Some(tapadNexusSnapshots)
  else
    Some(tapadNexusReleases)
}

publishMavenStyle := true

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.2.0")

scalariformSettings
