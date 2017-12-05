package com.github.ddemin.envrouter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsNot.not;

import com.github.ddemin.envrouter.base.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import java.util.Properties;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EnvironmentTests extends UnitTestsBase {

  private static final String ENV1 = "test1";
  private static final String ENV2 = "test2";

  @Test
  public void checkConstructor() {
    System.setProperty("some.global.prop", "123");
    Environment env = new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV1));
    assertThat(
        "Environment must have properties from files that stored in environments directory",
        env.getProperties(),
        hasKey("some.global.prop")
    );
    assertThat(
        "Environment must have properties from files that stored in its personal directory",
        env.getProperties(),
        hasKey("test.step1")
    );
    assertThat(
        "Environment must haven't properties from files that stored in subdirectories of its personal directory",
        env.getProperties(),
        not(hasKey("some.another.prop"))
    );
    assertThat(
        "Environment must haven't properties from files that stored in parent directory of its personal directory",
        env.getProperties(),
        not(hasKey("common.property"))
    );
  }

  @Test
  public void checkEqualsMethod() {
    Environment env1 = new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV1));
    Environment env11 = new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV1));
    Environment env2 = new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV2));
    assertThat(env1, equalTo(env11));
    assertThat(env1, not(equalTo(env2)));
  }

  @Test
  public void checkPropertiesAdding() {
    Environment env = new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV1));
    Properties properties = new Properties();
    properties.put("test", "1");
    env.withProperties(properties);
    assertThat(env.getProperties(), hasKey("test"));
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "pathToPropertiesDir")
  public void catchNpe() {
    new Environment(null);
  }


}
