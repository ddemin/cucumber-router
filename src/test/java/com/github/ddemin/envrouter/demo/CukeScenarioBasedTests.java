package com.github.ddemin.envrouter.demo;

import static com.github.ddemin.testutil.allure2.AllureUtils.saveAsBrokenTest;
import static java.lang.String.format;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.cucumber2.ScenarioWrapper;
import com.github.ddemin.envrouter.cucumber2.testng.AbstractCucumberScenarioTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

@CucumberOptions(
    glue = {"com.github.ddemin.envrouter.demo.step"},
    features = {"src/test/resources/features"},
    plugin = {"io.qameta.allure.cucumber2jvm.AllureCucumber2Jvm", "pretty"}
)
public class CukeScenarioBasedTests extends AbstractCucumberScenarioTest {

  @Factory(dataProvider = "routerDataProvider")
  public CukeScenarioBasedTests() {
  }

  @Test(enabled = true, groups = "demoViaScenarios")
  public void runTest() throws Throwable {
    super.testCucumberEntity();
  }

  @Override
  protected void processFailedLocking(EnvironmentLock<ScenarioWrapper> lock) {
    ScenarioWrapper wrapper = lock.getTargetEntity();
    String errorMessage;
    switch (lock.getLockStatus()) {
      case FAILURE_UNDEFINED_ENV:
        errorMessage = format(
            "Feature %s has undefined environment %s",
            wrapper.getEntity().uri,
            wrapper.getRequiredEnvironmentName()
        );
        saveAsBrokenTest(
            wrapper.getEntity().uri,
            errorMessage
        );
        throw new RuntimeException(errorMessage);
      case FAILURE_NO_TARGET_ENTITIES:
        errorMessage = "No any waiting feature was found for all available environments";
        saveAsBrokenTest(
            "Tests router global error",
            errorMessage
        );
        throw new IllegalStateException(errorMessage);
      default:
        errorMessage = "Unexpected lock status: " + lock.getLockStatus();
        saveAsBrokenTest(
            "Tests router global error",
            errorMessage
        );
        throw new IllegalStateException(errorMessage);
    }
  }

}
