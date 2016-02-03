import java.io.PrintWriter
import java.net.ServerSocket

object BasicApp extends App {

  val text =
    """HTTP/1.0 200 OK
        Content-Type: text/html
        Content-Length: 200

        <HTML> <HEAD> <TITLE>Hello, World!</TITLE> </HEAD> <BODY LANG="en-US" BGCOLOR="#e6e6ff" DIR="LTR"> <P ALIGN="CENTER"> <FONT FACE="Arial, sans-serif" SIZE="6">Hello, World!</FONT> </P> </BODY> </HTML>"""
  val port = 8080
  val listener = new ServerSocket(port)

  while (true) {
    val sock = listener.accept()
    new PrintWriter(sock.getOutputStream, true).println(text)
    sock.shutdownOutput()
  }
}