package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.RouterTestsUtils.changeRouterConfigConstants;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys;
import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

@Test(groups = "unit", singleThreaded = true, priority = 2)
public class EnvsLocksControllerTests {

  private static final String ENV1 = "test1";

  @Test
  public void checkConstructorAndGetterByName() {
    EnvsLocksController<TestEntityWrapper<String>> envsLocksController = new EnvsLocksController<>();

    assertThat(
        envsLocksController.getAll(),
        hasSize(3)
    );
    assertThat(
        envsLocksController.getAll().stream().map(Environment::getName).collect(Collectors.toList()),
        containsInAnyOrder("test1", "test2", "test3")
    );

    assertThat(
        "Environment constructed in controller must have properties from files that stored in common directory",
        envsLocksController.getByName(ENV1).getProperties(),
        hasKey("common.property")
    );
    assertThat(
        "Environment constructed in controller must have correct path",
        envsLocksController.getByName(ENV1).getPathToPropertiesDir().toString(),
        endsWith(ENV1)
    );
    assertThat(
        "Environment constructed in controller must have name",
        envsLocksController.getByName(ENV1).getName(),
        equalTo(ENV1)
    );
  }

  @Test(
      expectedExceptions = IllegalStateException.class,
      expectedExceptionsMessageRegExp = ".*" + RouterConfigKeys.ENVS_DIRECTORY_KEY,
      priority = 998
  )
  @SuppressFBWarnings
  public void checkConstructorForEmptyDir() throws IllegalAccessException {
    changeRouterConfigConstants("ENVS_DIRECTORY", "features");
    new EnvsLocksController<>();
  }

  @Test(
      expectedExceptions = Exception.class,
      expectedExceptionsMessageRegExp = ".*abcdef.*",
      priority = 999
  )
  @SuppressFBWarnings
  public void checkConstructorForIncorrectDir() throws IllegalAccessException {
    changeRouterConfigConstants("ENVS_DIRECTORY", "abcdef");
    new EnvsLocksController<>();
  }


}
