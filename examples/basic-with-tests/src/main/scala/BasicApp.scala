import scala.Console._
import java.io.PrintWriter
import java.net.ServerSocket

object BasicApp extends App {

  val responseText = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: 200\r\n\r\n<HTML><HEAD> <TITLE>Hello, world!</TITLE></HTML>"

  // This is the container port that the Dokcer container uses internally. This port will be dynamically assigned and
  // exposed for external access by Docker Compose.
  val containerPort = 8080

  val listener = new ServerSocket(containerPort)
  println(s"Container listening at port $containerPort")

  while (true) {
    val sock = listener.accept()
    new PrintWriter(sock.getOutputStream, true).println(responseText)
    sock.close()
  }
}