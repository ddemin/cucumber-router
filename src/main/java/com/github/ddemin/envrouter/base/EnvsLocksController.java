package com.github.ddemin.envrouter.base;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Slf4j
public class EnvsLocksController<T extends TestEntityWrapper> {

  private final Map<Environment, Integer> testDatasLockMap;
  private final int maxThreadsPerTestData;

  /**
   * Creates controller that handle availability of environments for usage in demo threads.
   *
   * @param envsSupplier supplier of Environment objects
   * @param maxThreadsPerTestData max amount of parallel demo threads that can use same environment
   */
  public EnvsLocksController(
      @NonNull Supplier<Set<Environment>> envsSupplier,
      int maxThreadsPerTestData
  ) {
    log.debug("Create lock controller...");
    this.testDatasLockMap = new ConcurrentHashMap<>(
        Maps.asMap(envsSupplier.get(), e -> maxThreadsPerTestData)
    );
    this.maxThreadsPerTestData = maxThreadsPerTestData;
  }

  /**
   * Creates controller that handle availability of environment for usage in demo threads.
   *
   * @param env environment
   * @param maxThreadsPerTestData max amount of parallel demo threads that can use same environment
   */
  public EnvsLocksController(
      @NonNull Environment env,
      int maxThreadsPerTestData
  ) {
    log.debug("Create lock controller for one environment: {}", env);
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
   * Check availability of environment for usage in demo thread.
   *
   * @param env environment
   * @return possibility to use this environment in one more demo thread
   */
  @Synchronized
  public boolean isAvailable(@NonNull Environment env) {
    return testDatasLockMap.get(env) > 0;
  }

  /**
   * Check availability of environment for usage in demo thread.
   *
   * @param envName name of environment
   * @return possibility to use this environment in one more demo thread
   */
  @Synchronized
  public boolean isAvailable(@NonNull String envName) {
    Environment td = getByName(envName);
    return td != null && isAvailable(td);
  }

  /**
   * Returns set of all environments available for usage in demo threads.
   *
   * @return set of all environments that can be used in one more demo thread
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
    log.debug("Lock environment: {}", env);
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
    log.debug("Release environment: {}", env);
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
   *
   * @param envName environment name
   * @return environment object
   */
  public Environment getByName(@NonNull String envName) {
    return getAll().stream()
        .filter(it -> it.getName().equalsIgnoreCase(envName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Find untested entity and try to lock appropriate environment for it.
   * @param envQueues queues with untested entities
   * @return environment-for-entity lock with some status
   */
  public @NonNull EnvironmentLock<T> findUntestedEntityAndAssignEnv(@NonNull TestEntitiesQueues<T> envQueues) {
    return await()
        .timeout(RouterConfig.LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .until(
            () -> lockEnvAndSelectEntity(envQueues),
            hasProperty(
                "lockStatus",
                not(
                    anyOf(
                        is(LockStatus.FAILURE_NO_AVAILABLE),
                        is(LockStatus.FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS)
                    )
                )
            )
        );
  }

  @Synchronized
  private @NonNull EnvironmentLock<T> lockEnvAndSelectEntity(@NonNull TestEntitiesQueues<T> queues) {
    List<Entry<String, Queue<T>>> queuesForUndefEnvs = queues.getQueuesForUndefinedEnvs(getAll());
    Set<Environment> availableEnvs = getAllAvailable();
    T entityForTest = null;
    Environment envForTest = null;

    if (queues.entitiesInAllQueues() <= 0) {
      log.error("No any entities in queues");
      return new EnvironmentLock<>(LockStatus.FAILURE_NO_TARGET_ENTITIES);
    } else if (queuesForUndefEnvs.size() > 0) {
      T entity = queuesForUndefEnvs.get(0).getValue().poll();
      log.warn("Entity for undefined env was found: {}", entity);
      return new EnvironmentLock<>(
          null,
          entity,
          LockStatus.FAILURE_UNDEFINED_ENV
      );
    } else if (availableEnvs.size() <= 0) {
      log.info("No any available environments");
      return new EnvironmentLock<>(LockStatus.FAILURE_NO_AVAILABLE);
    } else {
      log.debug("Search untested entities for available environments...");
      for (Environment env : availableEnvs) {
        entityForTest = queues.pollEntityFor(env.getName());
        if (entityForTest != null) {
          envForTest = env;
          break;
        }
      }
    }

    if (entityForTest != null && lock(envForTest)) {
      log.info(
          "Untested entity was found: {} and environment was locked: {}",
          entityForTest,
          envForTest
      );
      return new EnvironmentLock<>(envForTest, entityForTest, LockStatus.SUCCESS_LOCKED);
    } else {
      log.error("Something goes wrong");
      return new EnvironmentLock<>(LockStatus.FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS);
    }
  }

}
