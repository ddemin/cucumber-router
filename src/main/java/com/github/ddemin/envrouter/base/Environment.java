package com.github.ddemin.envrouter.base;

import com.github.ddemin.testutil.io.PropertiesUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    log.debug("Create environment based on folder: {}", pathToPropertiesDir);
    this.pathToPropertiesDir = pathToPropertiesDir;
    this.name = pathToPropertiesDir.getFileName().toString();

    this.properties = new Properties();
    withProperties(System.getProperties());
    withProperties(PropertiesUtils.readProperties(pathToPropertiesDir.toUri()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
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

  public void withProperties(Map map) {
    this.properties.putAll(map);
  }

}
