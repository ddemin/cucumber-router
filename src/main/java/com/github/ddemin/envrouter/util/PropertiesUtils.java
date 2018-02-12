package com.github.ddemin.envrouter.util;

import static com.github.ddemin.envrouter.util.FileSystemUtils.getFilePaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 27.10.2017.
 */
@Slf4j
public class PropertiesUtils {

  /**
   * Scan directory, get all properties files and read properties.
   *
   * @param pathToDirectory path to parent directory
   * @return properties from files
   */
  public static Properties readProperties(URI pathToDirectory) {
    log.debug("Get properties files from '{}'", pathToDirectory);

    Properties properties = new Properties();
    getFilePaths(pathToDirectory, ".*\\.properties").forEach(
        path -> {
          try {
            @Cleanup InputStream fileInputStream
                = Files.newInputStream(path, StandardOpenOption.READ);
            properties.load(fileInputStream);
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
    );
    return properties;
  }

}
