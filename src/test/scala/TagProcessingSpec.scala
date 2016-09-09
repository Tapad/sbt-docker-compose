import com.tapad.docker.DockerComposePlugin._
import org.scalatest.{BeforeAndAfter, FunSuite, OneInstancePerTest}

class TagProcessingSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest {

  val imageNoTag = "testImage"
  val imageLatestTag = "testImage:latest"
  val imageWithTag = "testImage:tag"
  val imagePrivateRegistryNoTag = "registry/testImage"
  val imagePrivateRegistryWithLatest = "registry/testImage:latest"
  val imagePrivateRegistryWithTag = "registry/testImage:tag"
  val imagePrivateRegistryWithOrgNoTag = "registry/org/testImage"
  val imagePrivateRegistryWithOrgWithTag = "registry/org/testImage:tag"
  val imageCustomTag = "testImage<localbuild>"
  val imageTagAndCustomTag = "testImage:latest<localbuild>"

  // Boundary
  val badImageWithColon = "testImage:"
  val badImageWithMultipleColon = "testImage:fooImage:latest"
  val badImageWithOnlyColon = ":::::::"

  test("Validate various image tag formats are properly replaced") {
    val replacementTag = "replaceTag"
    assert(replaceDefinedVersionTag(imageNoTag, replacementTag) == imageNoTag)

    assert(replaceDefinedVersionTag(imageLatestTag, replacementTag) == imageLatestTag)

    assert(replaceDefinedVersionTag(imageWithTag, replacementTag) == s"testImage:$replacementTag")

    assert(replaceDefinedVersionTag(imagePrivateRegistryNoTag, replacementTag) == imagePrivateRegistryNoTag)

    assert(replaceDefinedVersionTag(imagePrivateRegistryWithLatest, replacementTag) == imagePrivateRegistryWithLatest)

    assert(replaceDefinedVersionTag(imagePrivateRegistryWithTag, replacementTag) == s"registry/testImage:$replacementTag")
  }

  test("Validate image tag retrieval from various formats") {
    assert(getTagFromImage(imageNoTag) == "latest")

    assert(getTagFromImage(imageLatestTag) == "latest")

    assert(getTagFromImage(imageWithTag) == "tag")

    assert(getTagFromImage(imagePrivateRegistryNoTag) == "latest")

    assert(getTagFromImage(imagePrivateRegistryWithLatest) == "latest")

    assert(getTagFromImage(imagePrivateRegistryWithTag) == "tag")
  }

  test("Validate custom tags get removed") {
    assert(processImageTag(null, null, imageCustomTag) == "testImage")
    assert(processImageTag(null, null, imageTagAndCustomTag) == "testImage:latest")
  }

  test("Validate the removal of a tag from various image formats") {
    assert(getImageNameOnly(imageNoTag) == imageNoTag)
    assert(getImageNameOnly(imageLatestTag) == "testImage")
    assert(getImageNameOnly(imagePrivateRegistryNoTag) == "testImage")
    assert(getImageNameOnly(imagePrivateRegistryWithLatest) == "testImage")
    assert(getImageNameOnly(imagePrivateRegistryWithTag) == "testImage")
    assert(getImageNameOnly(imagePrivateRegistryWithOrgWithTag) == "testImage")
    assert(getImageNameOnly(imagePrivateRegistryWithOrgWithTag, removeOrganization = false) == "org/testImage")
  }

  test("Validate getting image name with no tag") {
    assert(getImageNoTag("") == "")
    assert(getImageNoTag(imageNoTag) == imageNoTag)
    assert(getImageNoTag(imageLatestTag) == imageNoTag)
    assert(getImageNoTag(imagePrivateRegistryNoTag) == imagePrivateRegistryNoTag)
    assert(getImageNoTag(imagePrivateRegistryWithLatest) == imagePrivateRegistryNoTag)
    assert(getImageNoTag(imagePrivateRegistryWithTag) == imagePrivateRegistryNoTag)
    assert(getImageNoTag(imagePrivateRegistryWithOrgWithTag) == imagePrivateRegistryWithOrgNoTag)
    assert(getImageNoTag(badImageWithColon) == imageNoTag)
    assert(getImageNoTag(badImageWithMultipleColon) == badImageWithMultipleColon.split(":").dropRight(1).mkString(":"))
    assert(getImageNoTag(badImageWithOnlyColon) == badImageWithOnlyColon.dropRight(1))
  }
}