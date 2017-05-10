import org.scalatest._
import scala.Console._
import scala.sys.process._
import scalaj.http.Http
import org.scalatest.Tag
import org.scalatest.concurrent._
import org.scalatest.exceptions._
import java.io.{ByteArrayOutputStream, PrintWriter}

class PerformanceSpec extends fixture.FunSuite with fixture.ConfigMapFixture with Eventually with IntegrationPatience with Matchers {

  test("Peformance Test: Template'") {
    configMap =>{
      println(s"Add performance testing code here...")
    }
  }

}
