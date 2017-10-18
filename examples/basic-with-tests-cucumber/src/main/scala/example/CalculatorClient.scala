package example

import scala.sys.process._

case class CalculatorClient(hostPort: String) {

  def add(x: Int, y: Int) = curl("add", x, y)

  def subtract(x: Int, y: Int) = curl("subtract", x, y)

  private def curl(path: String, x: Int, y: Int) = {
    val url = s"${hostPort}/$path/$x/$y"
    println(s"curling $url")
    val output = s"curl $url".!!
    try {
      output.trim.toInt
    } catch{
      case nfe : NumberFormatException =>
        println(output)
        throw new Exception(s"$url responded with: >${output}< : $nfe", nfe)
    }
  }

}
