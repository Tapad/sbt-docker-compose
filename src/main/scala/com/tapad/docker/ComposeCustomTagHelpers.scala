package com.tapad.docker

import sbt._

import scala.collection._

/**
 * Set of helper functions for parsing Docker Compose "image:" values
 */
trait ComposeCustomTagHelpers {
  val useLocalBuildTag = "<localbuild>"
  val skipPullTag = "<skippull>"
  val latestVersion = "latest"

  /**
   * Function that reads plug-in defined "<customTag>" fields from the Docker Compose file and performs some transformation on the Docker File based on the tag.
   * The file after transformations are applied is what is used by Docker Compose to launch the instance. This function can be overridden in derived plug-ins to add
   * additional tags pre-processing features.
   * @param state The sbt state
   * @param args Args passed to sbt command
   * @param imageName The image name and tag to be processed for example "testimage:1.0.0<skipPull>" This plugin just removes the tags from the image name.
   * @return The updated image value after any processing indicated by the custom tags
   */
  def processImageTag(implicit state: State, args: Seq[String], imageName: String): String = {
    imageName.toLowerCase.replace(useLocalBuildTag, "").replace(skipPullTag, "")
  }

  /**
   * Parses the image name to get the "tag" value
   * @param imageName The full image name
   * @return Returns the "tag" value of on an image if it is defined. Otherwise, it will return "latest" as the tag.
   */
  def getTagFromImage(imageName: String): String = {
    val indexOfTag = imageName.lastIndexOf(':')
    if (indexOfTag == -1) {
      //If no tag is defined it is assumed to be the latest tag
      latestVersion
    } else {
      imageName.substring(indexOfTag + 1)
    }
  }

  /**
   * Replaces the currently defined tag with the newly specified one. If no tag exists or the "latest" tag is defined then just return the original image.
   * @param imageName The full image name
   * @param newTag The new tag to put on the image
   * @return The updated image name with the previous tag replaced by newly specified tag
   */
  def replaceDefinedVersionTag(imageName: String, newTag: String): String = {
    //Handle the case where the "latest" tag is used for the image. In this case disregard the sbt project version info
    if (imageName.endsWith(latestVersion) || !imageName.contains(":")) {
      imageName
    } else {
      imageName.substring(0, imageName.lastIndexOf(':')) + s":$newTag"
    }
  }

  /**
   * Returns the image name without a tag appended to it. It will also remove any Docker Registry information prepended on the image
   * @param imageName The full image name
   * @return The image name without any tag or Docker Registry information
   */
  def imageWithoutTag(imageName: String): String = {
    val indexOfTag = imageName.lastIndexOf(':')
    if (indexOfTag == -1) {
      imageName
    } else {
      val indexOfImageStart = imageName.indexOf('/') + 1
      imageName.substring(indexOfImageStart, indexOfTag)
    }
  }
}
