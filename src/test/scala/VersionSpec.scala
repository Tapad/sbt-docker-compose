import com.tapad.docker.Version
import org.scalatest.{BeforeAndAfter, FunSuite, OneInstancePerTest}

class VersionSpec extends FunSuite with BeforeAndAfter with OneInstancePerTest with MockHelpers {
  test("Validate version information is parsed correctly") {
    assert(Version.parseVersion("1.0.0") == Version(1, 0, 0))
    assert(Version.parseVersion("11.1.1") == Version(11, 1, 1))
    assert(Version.parseVersion("1.0.0-SNAPSHOT") == Version(1, 0, 0))
    assert(Version.parseVersion("1.2.3") == Version(1, 2, 3))
    assert(Version.parseVersion("1.2.3-rc3") == Version(1, 2, 3))
    assert(Version.parseVersion("1.2.3rc3") == Version(1, 2, 3))
  }

  test("Validate invalid version information reports an exception") {
    intercept[RuntimeException] {
      Version.parseVersion("")
    }

    intercept[RuntimeException] {
      Version.parseVersion("1.0")
    }

    intercept[RuntimeException] {
      Version.parseVersion("-1.0")
    }

    intercept[RuntimeException] {
      Version.parseVersion("version")
    }
  }
}