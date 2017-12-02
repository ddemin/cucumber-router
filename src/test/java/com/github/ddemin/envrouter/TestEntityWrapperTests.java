package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.RouterTestsUtils.changeRouterConfigConstants;
import static com.github.ddemin.envrouter.base.TestEntityWrapper.ANY_ENV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.ddemin.envrouter.base.TestEntityWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.testng.annotations.Test;

@Test(groups = "unit", singleThreaded = true)
public class TestEntityWrapperTests {

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "entity")
  public void catchNpeForNullInConstructor() {
    new TestEntityWrapper<String>(null, "demo", 1);
  }

  @Test
  public void checkUsageOfAnyEnv() {
    TestEntityWrapper wrapper = new TestEntityWrapper<>("entity", null, 1);
    assertThat(
        wrapper.getRequiredEnvironmentName(),
        equalTo(ANY_ENV)
    );
  }

  @Test
  public void checkUsageOfDefaultEnv() throws IllegalAccessException {
    changeRouterConfigConstants("ENV_DEFAULT", "test1");
    TestEntityWrapper wrapper = new TestEntityWrapper<>("entity", null, 1);
    assertThat(
        wrapper.getRequiredEnvironmentName(),
        equalTo("test1")
    );
  }

}
