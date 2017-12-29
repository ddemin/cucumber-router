package com.github.ddemin.envrouter.base;

import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_NO_AVAILABLE;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_NO_TARGET_ENTITIES;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_UNDEFINED_ENV;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.SUCCESS_LOCKED;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys;
import com.google.common.collect.Maps;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Slf4j
@Getter
public class EnvsLocksController<T extends TestEntityWrapper> {

  private final Map<Environment, Integer> testDatasLockMap;

  /**
   * Creates controller that handle availability of environments for usage in demo threads. ENVS_DIRECTORY and
   * ENV_THREADS_MAX from RouterConfig will be used.
   */
  public EnvsLocksController() {
    log.debug("Create lock controller...");
    this.testDatasLockMap = new TreeMap<>((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    this.testDatasLockMap.putAll(
        Maps.asMap(
            EnvironmentsUtils.initAllFromDirectory(RouterConfig.ENVS_DIRECTORY),
            e -> RouterConfig.ENV_THREADS_MAX
        )
    );
    if (this.testDatasLockMap.isEmpty()) {
      throw new IllegalStateException(
          "No any environments were found and initiated, check property: "
              + RouterConfigKeys.ENVS_DIRECTORY_KEY
      );
    }
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
        .collect(Collectors.toCollection(LinkedHashSet::new));
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
    if (envFreeThreads < RouterConfig.ENV_THREADS_MAX) {
      testDatasLockMap.put(env, envFreeThreads + 1);
    }
  }

  /**
   * Reset locking of environment.
   *
   * @param env environment for reset
   */
  @Synchronized
  public void resetLock(@NonNull Environment env) {
    log.debug("Reset locking of environment: {}", env);
    testDatasLockMap.put(env, RouterConfig.ENV_THREADS_MAX);
  }

  /**
   * Release all environments.
   */
  @Synchronized
  public void releaseAll() {
    testDatasLockMap.keySet().forEach(this::release);
  }

  /**
   * Reset locking of all environments.
   */
  @Synchronized
  public void resetLockingOfAll() {
    testDatasLockMap.keySet().forEach(this::resetLock);
  }

  /**
   * Find untested entity and try to lock appropriate environment for it.
   *
   * @param envQueues queues with untested entities
   * @return environment-for-entity lock with some status
   */
  public @NonNull EnvironmentLock<T> findUntestedEntityAndLockEnv(@NonNull TestEntitiesQueues<T> envQueues) {
    return await()
        .pollInSameThread()
        .timeout(RouterConfig.LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(
            () -> findEntityAndLockEnv(envQueues),
            hasProperty(
                "lockStatus",
                not(
                    anyOf(
                        is(FAILURE_NO_AVAILABLE),
                        is(FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS)
                    )
                )
            )
        );
  }

  @Synchronized
  private @NonNull EnvironmentLock<T> findEntityAndLockEnv(@NonNull TestEntitiesQueues<T> queues) {
    List<Entry<String, Queue<T>>> queuesForUndefEnvs = queues.getQueuesForUndefinedEnvs(getAll());
    Set<Environment> availableEnvs = getAllAvailable();
    T entityForTest = null;
    Environment envForTest = null;

    if (queues.entitiesInAllQueues() <= 0) {
      log.error("No any entities in queues");
      return new EnvironmentLock<>(FAILURE_NO_TARGET_ENTITIES);
    } else if (queuesForUndefEnvs.size() > 0) {
      T entity = queuesForUndefEnvs.get(0).getValue().poll();
      log.warn("Entity for undefined env was found: {}", entity);
      return new EnvironmentLock<>(null, entity, FAILURE_UNDEFINED_ENV);
    } else if (availableEnvs.size() <= 0) {
      log.info("No any available environments");
      return new EnvironmentLock<>(FAILURE_NO_AVAILABLE);
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
      return new EnvironmentLock<>(envForTest, entityForTest, SUCCESS_LOCKED);
    } else {
      return new EnvironmentLock<>(FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS);
    }
  }

}
