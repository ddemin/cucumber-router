package com.github.ddemin.envrouter.base;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Slf4j(topic = "td")
public class EnvironmentLocksController {

  private final Map<Environment, Integer> testDatasLockMap;
  private final int maxThreadsPerTestData;

  /**
   * Creates controller that handle availability of environments for usage in test threads.
   *
   * @param envsSupplier supplier of Environment objects
   * @param maxThreadsPerTestData max amount of parallel test threads that can use same environment
   */
  public EnvironmentLocksController(
      @NonNull Supplier<Set<Environment>> envsSupplier,
      int maxThreadsPerTestData
  ) {
    this.testDatasLockMap = new ConcurrentHashMap<>(
        Maps.asMap(envsSupplier.get(), e -> maxThreadsPerTestData)
    );
    this.maxThreadsPerTestData = maxThreadsPerTestData;
  }

  /**
   * Creates controller that handle availability of environment for usage in test threads.
   *
   * @param env environment
   * @param maxThreadsPerTestData max amount of parallel test threads that can use same environment
   */
  public EnvironmentLocksController(
      @NonNull Environment env,
      int maxThreadsPerTestData
  ) {
    this.testDatasLockMap = new HashMap<>();
    this.testDatasLockMap.put(env, maxThreadsPerTestData);
    this.maxThreadsPerTestData = maxThreadsPerTestData;
  }

  /**
   * Returns set of all environments provided to this controller.
   *
   * @return set of environments
   */
  public Set<Environment> getAll() {
    return testDatasLockMap.keySet();
  }

  /**
   * Check availability of environment for usage in test thread.
   *
   * @param env environment
   * @return possibility to use this environment in one more test thread
   */
  @Synchronized
  public boolean isAvailable(@NonNull Environment env) {
    return testDatasLockMap.get(env) > 0;
  }

  /**
   * Check availability of environment for usage in test thread.
   *
   * @param envName name of environment
   * @return possibility to use this environment in one more test thread
   */
  @Synchronized
  public boolean isAvailable(@NonNull String envName) {
    Environment td = getByName(envName);
    return td != null && isAvailable(td);
  }

  /**
   * Returns set of all environments available for usage in test threads.
   *
   * @return set of all environments that can be used in one more test thread
   */
  @Synchronized
  public Set<Environment> getAllAvailable() {
    return testDatasLockMap.entrySet().stream()
        .map(Entry::getKey)
        .filter(this::isAvailable)
        .collect(Collectors.toSet());
  }

  /**
   * Try to lock environment.
   *
   * @param env environment for locking
   * @return success of environment lock
   */
  @Synchronized
  public boolean lock(@NonNull Environment env) {
    if (!isAvailable(env)) {
      return false;
    }
    testDatasLockMap.put(env, testDatasLockMap.get(env) - 1);
    return true;
  }

  /**
   * Release environment.
   *
   * @param env environment for release
   */
  @Synchronized
  public void release(@NonNull Environment env) {
    int envFreeThreads = testDatasLockMap.get(env);
    if (envFreeThreads < maxThreadsPerTestData) {
      testDatasLockMap.put(env, envFreeThreads + 1);
    }
  }

  /**
   * Release all environments.
   */
  @Synchronized
  public void releaseAll() {
    for (Environment td : testDatasLockMap.keySet()) {
      release(td);
    }
  }

  /**
   * Get environment from map by name.
   * @param envName environment name
   * @return environment object
   */
  public Environment getByName(@NonNull String envName) {
    return getAll().stream()
        .filter(it -> it.getName().equalsIgnoreCase(envName))
        .findFirst()
        .orElse(null);
  }

}
