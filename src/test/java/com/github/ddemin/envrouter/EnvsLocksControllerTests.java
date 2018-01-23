package com.github.ddemin.envrouter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys;
import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.stream.Collectors;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EnvsLocksControllerTests extends UnitTestsBase {

  private static final String ENV1 = "test1";

  public EnvsLocksControllerTests() {
    super();
    try {
      changeRouterConfigConstants("ENV_THREADS_MAX", 2);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeMethod
  public void releaseEnvs() throws IllegalAccessException {
    changeRouterConfigConstants("ENV_THREADS_MAX", 2);
    EnvsLocksController.resetLockingOfAll();
  }

  @Test
  public void checkLocking() throws IllegalAccessException {
    Environment env1 = EnvsLocksController.getByName(ENV1);

    assertThat(
        "1st locking must be successful (limit = 2)",
        EnvsLocksController.lock(env1),
        is(true)
    );
    assertThat(
        "Environment must be available after 1st lock (limit = 2)",
        EnvsLocksController.isAvailable(env1),
        is(true)
    );

    assertThat(
        "2nd locking must be successful (limit = 2)",
        EnvsLocksController.lock(env1),
        is(true)
    );
    assertThat(
        "Environment must be not  available after 2nd lock (limit = 2)",
        EnvsLocksController.isAvailable(env1),
        is(false)
    );

    assertThat(
        "3rd locking must be not successful (limit = 2)",
        EnvsLocksController.lock(env1),
        is(false)
    );
  }

  @Test
  public void checkReleasing() throws IllegalAccessException {
    changeRouterConfigConstants("ENV_THREADS_MAX", 2);
    Environment env1 = EnvsLocksController.getByName(ENV1);

    EnvsLocksController.lock(env1);
    EnvsLocksController.lock(env1);
    assertThat(
        "Environment must be not  available after 2nd lock (limit = 2)",
        EnvsLocksController.isAvailable(env1),
        is(false)
    );

    EnvsLocksController.release(env1);
    assertThat(
        "Environment must be available after releasing of 1 thread",
        EnvsLocksController.isAvailable(env1),
        is(true)
    );

    EnvsLocksController.lock(env1);
    assertThat(
        EnvsLocksController.isAvailable(env1),
        is(false)
    );

    EnvsLocksController.resetLock(env1);
    assertThat(
        EnvsLocksController.isAvailable(env1),
        is(true)
    );
    assertThat(
        EnvsLocksController.lock(env1),
        is(true)
    );
    assertThat(
        EnvsLocksController.lock(env1),
        is(true)
    );
  }

  @Test
  public void checkHardLocking() throws IllegalAccessException {
    Environment env1 = EnvsLocksController.getByName(ENV1);

    assertThat(
        "1st hard-locking must be successful",
        EnvsLocksController.hardLock(env1),
        is(true)
    );
    assertThat(
        "Environment must be not available after hard-lock",
        EnvsLocksController.isAvailable(env1),
        is(false)
    );
    assertThat(
        "2nd locking must be not successful",
        EnvsLocksController.lock(env1),
        is(false)
    );

    EnvsLocksController.release(env1);
    assertThat(
        "Environment must be available after releasing after hard-lock",
        EnvsLocksController.isAvailable(env1),
        is(true)
    );
    assertThat(
        "Locking must be successful (1/2)",
        EnvsLocksController.lock(env1),
        is(true)
    );
    assertThat(
        "Locking must be successful (2/2)",
        EnvsLocksController.lock(env1),
        is(true)
    );
  }

  @Test
  public void checkConstructorAndGetterByName() {
    assertThat(
        EnvsLocksController.getAll(),
        hasSize(3)
    );
    assertThat(
        EnvsLocksController.getAll().stream().map(Environment::getName).collect(Collectors.toList()),
        containsInAnyOrder("test1", "test2", "test3")
    );
    assertThat(
        "Environment that constructed in controller"
            + " must have properties from files that stored in common directory",
        EnvsLocksController.getByName(ENV1).getProperties(),
        hasKey("common.property")
    );
    assertThat(
        "Environment constructed in controller must have correct path",
        EnvsLocksController.getByName(ENV1).getPathToPropertiesDir().toString(),
        endsWith(ENV1)
    );
    assertThat(
        "Environment constructed in controller must have name",
        EnvsLocksController.getByName(ENV1).getName(),
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
    EnvsLocksController.reinit();
  }

  @Test(
      expectedExceptions = Exception.class,
      expectedExceptionsMessageRegExp = ".*abcdef.*",
      priority = 999
  )
  @SuppressFBWarnings
  public void checkConstructorForIncorrectDir() throws IllegalAccessException {
    changeRouterConfigConstants("ENVS_DIRECTORY", "abcdef");
    EnvsLocksController.reinit();
  }

}
