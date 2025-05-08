package runners;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"steps"},
        plugin = { "pretty"},
        monochrome = true
)
public class TestRunner {
}

//features tell the location of .feature files
//This glue connects our .feature files to our java code
//pretty is used for clear console outputs
//Monochrome= true allows printing only in single color