import com.tapad.docker.DockerImagePluginType
import com.tapad.docker.DockerImagePluginType._

name := "basic"

version := "1.0.0"

scalaVersion := "2.10.6"

enablePlugins(JavaAppPackaging, DockerComposePlugin)

dockerImageCreationPlugin := NativePackager