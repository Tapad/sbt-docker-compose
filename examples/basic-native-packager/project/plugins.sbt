//TODO Remove once published to Sonatype
resolvers += "Scala Tools Nexus" at "http://nexus.tapad.com:8080/nexus/content/groups/aggregate/"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.0.6")

addSbtPlugin("com.tapad" % "sbt-docker-compose" % "0.0.1-SNAPSHOT")