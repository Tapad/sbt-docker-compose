package com.tapad.docker

case class OutputTableRow(
  serviceName: String,
  hostWithPort: String,
  versionTag: String,
  imageSource: String,
  containerPort: String,
  containerId: String,
  isDebug: Boolean) extends Ordered[OutputTableRow] {

  def toStringList: List[String] =
    List(
      serviceName,
      hostWithPort,
      versionTag,
      imageSource,
      containerPort,
      containerId,
      if (isDebug) "DEBUG" else "")

  def compare(that: OutputTableRow): Int = {
    // Sort Order
    // - Service Name - Alphabetically
    // - isDebug - Debug Ports always go at the end
    // - Container Port - Sorted by port number from lowest to highest
    if (this.serviceName == that.serviceName) {
      if (this.isDebug == that.isDebug) {
        val leftContainerPort = this.containerPort.split("/").head.toInt
        val rightContainerPort = that.containerPort.split("/").head.toInt
        leftContainerPort compare rightContainerPort
      } else {
        this.isDebug compare that.isDebug
      }
    } else {
      this.serviceName compare that.serviceName
    }
  }
}
