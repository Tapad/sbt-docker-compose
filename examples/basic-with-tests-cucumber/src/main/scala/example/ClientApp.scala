package example

object ClientApp extends App {
  val client = CalculatorClient("http://localhost:8080")

  println(client.add(5, 6))
  println(client.subtract(5, 6))

}
