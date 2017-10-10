package example

import java.nio.file.{Files, Path, Paths}

object Calculator {

  def main(args: Array[String]): Unit = {
    args.toList match {
      case Nil => println(s"Usage: Expected either two ints or a file path")
      case List(filePath) =>
        println(apply(Paths.get(filePath)))
      case List(a, b) =>
        println(add(a.toInt, b.toInt))
      case err => println(s"Usage: Expected either two ints or a file path, but got $err")
    }
  }

  def add(x: Int, y: Int): Int = x + y

  def subtract(x: Int, y: Int): Int = x - y

  val PlusR = """(\d+)\s*\+\s*(\d+)""".r
  val MinusR = """(\d+)\s*-\s*(\d+)""".r

  /**
    * Evaluates the first line of the input file to be an addition or subtration operation.
    *
    * This is just added to demonstrate e.g. composing two containers w/ shared volumes
    */
  def apply(input: Path): Int = {
    import scala.collection.JavaConverters._
    Files.readAllLines(input).asScala.head match {
      case PlusR(a, b) => add(a.toInt, b.toInt)
      case MinusR(a, b) => subtract(a.toInt, b.toInt)
      case _ => sys.error("Whacha talkin' bout, willis?")
    }
  }

}
