package com.tapad.docker

/**
 * Set of helper functions for parsing Docker Compose "image:" values
 */
trait ComposeCustomTagHelpers {
  val latestVersion = "latest"

  /**
   * Parses the image name to get the "tag" value
   * @param imageName The full image name
   * @return Returns the "tag" value of on an image if it is defined. Otherwise, it will return "latest" as the tag.
   */
  def getTagFromImage(imageName: String): String = imageName.lastIndexOf(':') match {
    case -1 => latestVersion
    case indexOfTag => imageName.substring(indexOfTag + 1)
  }

  /**
   * Replaces the currently defined tag with the newly specified one. If no tag exists or the "latest" tag is defined
   * then just return the original image.
   * @param imageName The full image name
   * @param newTag The new tag to put on the image
   * @return The updated image name with the previous tag replaced by newly specified tag
   */
  def replaceDefinedVersionTag(imageName: String, newTag: String): String = (imageName.lastIndexOf(":"), imageName.endsWith(s":$latestVersion")) match {
    //Handle the case where the "latest" tag is used for the image. In this case disregard the sbt project version info
    case (-1, _) => imageName
    case (_, true) => imageName
    case (index, false) => s"${imageName.substring(0, index)}:$newTag"
  }

  /**
   * Remove tag from image name if it exists.
   * @param imageName The full image name.
   * @return The updated image name with the tag removed.
   */
  def getImageNoTag(imageName: String): String = imageName.lastIndexOf(':') match {
    case -1 => imageName
    case index => imageName.substring(0, index)
  }

  /**
   * Returns the image name without a tag, organization info or Docker Registry information. With the image format being:
   * registry/org/image:tag
   * this function will return "image" or "org/image" if removeOrganization is false.
   * @param imageName The full image name
   * @param removeOrganization True to remove organization info, False to keep it. Default is True.
   * @return
   */
  def getImageNameOnly(imageName: String, removeOrganization: Boolean = true): String = {
    val imageNoTag = getImageNoTag(imageName)

    //If there is no registry than return return image without a tag
    (imageNoTag.indexOf('/'), removeOrganization) match {
      case (-1, _) => imageNoTag
      case (_, true) => imageNoTag.substring(imageNoTag.lastIndexOf('/') + 1)
      case (indexOfRegistryEnd, false) => imageNoTag.substring(indexOfRegistryEnd + 1)
    }
  }
}
