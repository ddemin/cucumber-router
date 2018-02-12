package com.github.ddemin.envrouter.base;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.cucumber2.testng.AbstractCucumberFeatureTest;
import com.github.ddemin.envrouter.util.FileSystemUtils;
import com.github.ddemin.envrouter.util.PropertiesUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@UtilityClass
public class EnvironmentsUtils {

  private static final ThreadLocal<Environment> tlEnvs = ThreadLocal.withInitial(() -> null);

  /**
   * Create set of Environments after scanning of default directory with environments subdir-s.
   *
   * @return environment objects that based on subdir-s in provided directory
   */
  public static Set<Environment> getAllEnvironments() {
    return getAllEnvironmentsFromDirectory(RouterConfig.ENVS_DIRECTORY);
  }

  /**
   * Create set of Environments after scanning of directory with subdir-s.
   *
   * @param pathToDir path to directory with subdir-s
   * @return environment objects that based on subdir-s in provided directory
   */
  public static Set<Environment> getAllEnvironmentsFromDirectory(String pathToDir) {
    log.debug("Find environments directories in {}", pathToDir);

    URL dirUrl = AbstractCucumberFeatureTest.class.getClassLoader().getResource(pathToDir);
    if (dirUrl == null) {
      throw new IllegalArgumentException("Directory isn't found: " + pathToDir);
    }

    URI dirUri;
    try {
      dirUri = dirUrl.toURI();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Directory URL can't be converted to URI: " + e.getMessage(), e);
    }

    return FileSystemUtils.getSubdirectories(dirUri).stream()
        .map(path -> {
          Environment env = new Environment(path);
          try {
            env.withProperties(
                PropertiesUtils.readProperties(
                    EnvironmentsUtils.class
                        .getClassLoader()
                        .getResource(pathToDir)
                        .toURI()
                )
            );
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          }
          return env;
        })
        .collect(Collectors.toSet());
  }

  /**
   * Returns environment for current thread.
   *
   * @return environment that assigned for current thread
   */
  public static Environment getCurrent() {
    Environment env = tlEnvs.get();
    log.trace("Current environment: {}", env);
    return env;
  }

  /**
   * Set environment for current thread.
   *
   * @param env environment that will be assigned for current thread
   */
  public static void setCurrent(Environment env) {
    log.debug("Set current environment: {}", env);
    tlEnvs.set(env);
  }

}
