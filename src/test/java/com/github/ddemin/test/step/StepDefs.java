package com.github.ddemin.test.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentsContext;
import cucumber.api.java.en.Given;
import io.qameta.allure.Step;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

@Slf4j
public class StepDefs {

  @Given("^Step 1$")
  public void step1() throws InterruptedException {
    log.info(getProperties().getProperty("test.step1"));

    assertThat("This assert must be successful", 7, greaterThan(6));

    allureStep(getProperties().getProperty("test.global"));
    sleep();
  }

  @Given("^Step 2$")
  public void step2() throws InterruptedException {
    log.info(getProperties().getProperty("test.step2"));
    sleep();
  }

  @Given("^Step 3$")
  public void step3() throws InterruptedException {
    log.info(getProperties().getProperty("test.step3"));
    sleep();
  }

  @Step("This is Allure step. Global property test.global = {globProperty}")
  private void allureStep(String globProperty) {
  }

  private void sleep() throws InterruptedException {
    Thread.sleep(RandomUtils.nextInt(333, 777));
  }

  private Properties getProperties() {
    Environment env = EnvironmentsContext.getCurrent();
    return env.getProperties();
  }

}
