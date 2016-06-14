import scala.Console._
import scala.concurrent.duration._

object BasicApp extends App {
  println("Application started....")

  val deadline = 1.hour.fromNow
  do {
    println(s"Running application. Seconds left until showdown: ${deadline.timeLeft.toSeconds}")
    Thread.sleep(1000)
  } while (deadline.hasTimeLeft())

  println("Application shutting down....")
}