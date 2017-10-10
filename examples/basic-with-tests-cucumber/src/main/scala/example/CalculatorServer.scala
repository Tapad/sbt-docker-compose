package example

import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

object CalculatorServer extends App {
  def start(port: Int) = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", Handler)
    server.start()
    server
  }


  object Handler extends HttpHandler {

    val AddInput = """/add/([-0-9]+)/([-0-9]+)/?""".r
    val SubtractInput = """/subtract/([-0-9]+)/([-0-9]+)/?""".r

    override def handle(ex: HttpExchange): Unit = {
      val path = ex.getRequestURI.toString

      val resultOpt = path match {
        case AddInput(a, b) => Option(Calculator.add(a.toInt, b.toInt))
        case SubtractInput(a, b) =>
          Option(Calculator.subtract(a.toInt, b.toInt))
        case _ => None
      }

      val replyString = resultOpt match {
        case Some(x) =>
          val response = x.toString
          ex.sendResponseHeaders(200, response.length)
          response
        case None =>
          val response = s"Unknown path: $path"
          ex.sendResponseHeaders(404, response.length)
          response
      }

      ex.getResponseBody.write(replyString.getBytes)
    }
  }


  val port = args.headOption.map(_.toInt).getOrElse(8080)
  println(s"Starting calculator server on port $port w/ user args ${args.mkString(": [", ",", "]")}")
  start(port)
}