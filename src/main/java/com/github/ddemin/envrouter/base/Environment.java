package com.github.ddemin.envrouter.base;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.cucumber2.testng.AbstractCucumberTest;
import com.github.ddemin.testutil.io.PropertiesUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;
import lombok.Data;
import lombok.NonNull;

@Data
public class Environment {

  private String name;
  private Path pathToPropertiesDir;
  private Properties properties;

  /**
   * Creates environment object that associated with some directory that contains config files.
   *
   * @param pathToPropertiesDir path to directory that associated with some environment
   */
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public Environment(@NonNull Path pathToPropertiesDir) {
    this.pathToPropertiesDir = pathToPropertiesDir;
    this.name = pathToPropertiesDir.getFileName().toString();
    this.properties = new Properties(System.getProperties());
    try {
      this.properties.putAll(
          PropertiesUtils.readProperties(
              AbstractCucumberTest.class
                  .getClassLoader()
                  .getResource(RouterConfig.ENVS_DIRECTORY)
                  .toURI()
          )
      );
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    this.properties.putAll(
        PropertiesUtils.readProperties(pathToPropertiesDir.toUri())
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    Environment that = (Environment) o;

    return pathToPropertiesDir.equals(that.pathToPropertiesDir);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + pathToPropertiesDir.hashCode();
    return result;
  }

}
