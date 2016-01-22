logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers += "Scala Tools Nexus" at "http://nexus.tapad.com:8080/nexus/content/groups/aggregate/"

addSbtPlugin("com.tapad.docker" % "sbt-docker-compose" % "0.0.1-SNAPSHOT")