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
   * Replaces the currently defined tag with the newly specified one. If no tag exists or the "latest" tag is defined
   * then just return the original image.
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
   * Returns the image name without a tag, organization info or Docker Registry information. With the image format being:
   * registry/org/image:tag
   * this function will return "image" or "org/image" if removeOrganization is false.
   * @param imageName The full image name
   * @param removeOrganization True to remove organization info, False to keep it. Default is True.
   * @return
   */
  def getImageNameOnly(imageName: String, removeOrganization: Boolean = true): String = {
    //Remove the tag if it exists
    val indexOfTag = imageName.lastIndexOf(':')
    val imageNoTag = if (indexOfTag == -1) {
      imageName
    } else {
      imageName.substring(0, indexOfTag)
    }

    val indexOfRegistryEnd = imageNoTag.indexOf('/')
    val indexOfOrganizationEnd = imageNoTag.lastIndexOf('/')
    val orgExists = indexOfRegistryEnd != indexOfOrganizationEnd

    //If there is no registry than return return image without a tag
    if (indexOfRegistryEnd == -1) {
      imageNoTag
    } else if (orgExists && removeOrganization) {
      imageNoTag.substring(indexOfOrganizationEnd + 1)
    } else {
      imageNoTag.substring(indexOfRegistryEnd + 1)
    }
  }
}
