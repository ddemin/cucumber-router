package com.github.ddemin.envrouter.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 27.10.2017.
 */
@Slf4j
public class FileSystemUtils {

  /**
   * Get paths of subdirectories (scan depth = 1).
   *
   * @param pathToDirectory path to parent directory
   * @return set of subdir-s paths
   */
  public static Set<Path> getSubdirectories(URI pathToDirectory) {
    log.debug("Get names of all 1st-level subdirectories in: {}", pathToDirectory);

    try (Stream<Path> paths = Files.walk(Paths.get(pathToDirectory), 1)) {
      return paths
          .filter(path -> Files.isDirectory(path))
          .filter(path -> {
            try {
              return !Files.isSameFile(path, Paths.get(pathToDirectory));
            } catch (IOException ex) {
              throw new RuntimeException(ex);
            }
          })
          .collect(Collectors.toSet());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Get paths to files that match some regex (scan depth = 1).
   *
   * @param pathToDirectory path to parent directory
   * @param filePattern regex
   * @return paths to files that match filePattern
   */
  public static List<Path> getFilePaths(URI pathToDirectory, String filePattern) {
    return getFilePaths(pathToDirectory, filePattern, 1);
  }

  /**
   * Get paths to files that match some regex.
   *
   * @param pathToDirectory path to parent directory
   * @param filePattern regex
   * @param depth scan depth
   * @return paths to files that match filePattern
   */
  public static List<Path> getFilePaths(URI pathToDirectory, String filePattern, int depth) {
    log.info("Find files '{}' in '{}' (depth {}) ...", filePattern, pathToDirectory, depth);
    try (Stream<Path> paths = Files.find(
        Paths.get(pathToDirectory),
        depth,
        (path, basicFileAttributes) ->
            Files.isRegularFile(path) && path.toFile().getName().matches(filePattern)
    )) {
      return paths
          .filter(path -> {
            log.info("File was found: {}", path.toString());
            return true;
          })
          .collect(Collectors.toList());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

  }

}
