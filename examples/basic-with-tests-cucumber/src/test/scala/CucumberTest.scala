package example

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
@CucumberOptions(
  features = Array("classpath:"),
  glue = Array("classpath:"),
  plugin = Array("pretty", "html:target/cucumber/report.html"),
  strict = true
)
class CucumberTest
