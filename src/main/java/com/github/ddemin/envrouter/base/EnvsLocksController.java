package com.github.ddemin.envrouter.base;

import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_NO_AVAILABLE;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_NO_TARGET_ENTITIES;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_TIMEOUT;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.FAILURE_UNDEFINED_ENV;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.SUCCESS_HARD_LOCKED;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.SUCCESS_LOCKED;
import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.Collection;
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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Slf4j
@Getter
@UtilityClass
public class EnvsLocksController {

  private static Map<Environment, Integer> testDatasLockMap;

  static {
    reinit();
  }

  /**
   * Creates controller that handle availability of environments for usage in demo threads. ENVS_DIRECTORY and
   * ENV_THREADS_MAX from RouterConfig will be used.
   */
  public static void reinit() {
    log.debug("Create lock controller...");
    testDatasLockMap = new TreeMap<>((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    testDatasLockMap.putAll(
        Maps.asMap(
            EnvironmentsUtils.initAllFromDirectory(RouterConfig.ENVS_DIRECTORY),
            e -> RouterConfig.ENV_THREADS_MAX
        )
    );
    if (testDatasLockMap.isEmpty()) {
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
  public static Set<Environment> getAll() {
    return testDatasLockMap.keySet();
  }

  /**
   * Get environment from map by name.
   *
   * @param envName environment name
   * @return environment object
   */
  public static Environment getByName(@NonNull String envName) {
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
  public static boolean isAvailable(@NonNull Environment env) {
    return testDatasLockMap.get(env) > 0;
  }

  /**
   * Check availability of environment for usage in demo thread.
   *
   * @param envName name of environment
   * @return possibility to use this environment in one more demo thread
   */
  @Synchronized
  public static boolean isAvailable(@NonNull String envName) {
    Environment td = getByName(envName);
    return td != null && isAvailable(td);
  }

  /**
   * Returns set of all environments available for usage in demo threads.
   *
   * @return set of all environments that can be used in one more demo thread
   */
  @Synchronized
  public static Set<Environment> getAllAvailable() {
    return testDatasLockMap.entrySet().stream()
        .map(Entry::getKey)
        .filter(EnvsLocksController::isAvailable)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Try to hard-lock environment. All slots will be used!
   *
   * @param env environment for locking
   * @return success of environment lock
   */
  @Synchronized
  public static boolean hardLock(@NonNull Environment env) {
    if (testDatasLockMap.get(env) < RouterConfig.ENV_THREADS_MAX) {
      return false;
    }
    log.debug("Hard-lock of environment: {}", env);
    testDatasLockMap.put(env, testDatasLockMap.get(env) * -1);
    return true;
  }

  /**
   * Try to lock environment.
   *
   * @param env environment for locking
   * @return success of environment lock
   */
  @Synchronized
  public static boolean lock(@NonNull Environment env) {
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
  public static void release(@NonNull Environment env) {
    log.debug("Release environment: {}", env);
    int envFreeThreads = testDatasLockMap.get(env);
    if (Math.abs(envFreeThreads) < RouterConfig.ENV_THREADS_MAX) {
      if (envFreeThreads < 0) {
        log.debug("Release after hard-lock: {}", env);
        testDatasLockMap.put(env, envFreeThreads * -1);
      } else {
        testDatasLockMap.put(env, envFreeThreads + 1);
      }
    } else {
      testDatasLockMap.put(env, RouterConfig.ENV_THREADS_MAX);
    }
  }

  /**
   * Reset locking of environment.
   *
   * @param env environment for reset
   */
  @Synchronized
  public static void resetLock(@NonNull Environment env) {
    log.debug("Reset locking of environment: {}", env);
    testDatasLockMap.put(env, RouterConfig.ENV_THREADS_MAX);
  }

  /**
   * Release all environments.
   */
  @Synchronized
  public static void releaseAll() {
    testDatasLockMap.keySet().forEach(EnvsLocksController::release);
  }

  /**
   * Reset locking of all environments.
   */
  @Synchronized
  public static void resetLockingOfAll() {
    testDatasLockMap.keySet().forEach(EnvsLocksController::resetLock);
  }

  /**
   * Find untested entity and try to lock appropriate environment for it.
   *
   * @param envQueues queues with untested entities
   * @return environment-for-entity lock with some status
   */
  public static <T extends TestEntityWrapper> EnvironmentLock<T> findUntestedEntityAndLockEnv(
      @NonNull TestEntitiesQueues<T> envQueues
  ) {
    EnvironmentLock<T> rez;
    try {
      rez = await()
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
    } catch (ConditionTimeoutException timeoutEx) {
      rez = new EnvironmentLock<>(FAILURE_TIMEOUT);
      List<T> untestedEntities = envQueues.getQueuesMap().values().stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
      rez.setStatusMessage(
          format(
              "Tests routing timeout occurred (%d ms). Untested entities:%n%s",
              RouterConfig.LOCK_TIMEOUT_MS,
              Joiner.on(System.lineSeparator()).join(untestedEntities))
      );
    }
    return rez;
  }

  @Synchronized
  private static <T extends TestEntityWrapper>  EnvironmentLock<T> findEntityAndLockEnv(
      @NonNull TestEntitiesQueues<T> queues
  ) {
    List<Entry<String, Queue<T>>> queuesForUndefEnvs = queues.getQueuesForUndefinedEnvs(getAll());
    Set<Environment> availableEnvs = getAllAvailable();
    T entityForTest;

    if (queues.entitiesInAllQueues() <= 0) {
      log.error("No any entities in queues");
      return new EnvironmentLock<>(FAILURE_NO_TARGET_ENTITIES);
    } else if (queuesForUndefEnvs.size() > 0) {
      T entity = queuesForUndefEnvs.get(0).getValue().poll();
      log.warn("Entity for undefined env was found: {}", entity);
      return new EnvironmentLock<>(null, entity, FAILURE_UNDEFINED_ENV, "");
    } else if (availableEnvs.size() <= 0) {
      log.info("No any available environments");
      return new EnvironmentLock<>(FAILURE_NO_AVAILABLE);
    } else {
      log.trace("Search untested entities for available environments...");
      for (Environment env : availableEnvs) {
        entityForTest = queues.pollEntityFor(env.getName());
        if (entityForTest == null) {
          continue;
        } else if (entityForTest.isRequiresHardLock() && hardLock(env)) {
          log.info(
              "Untested entity was found: {} and environment was HARD-locked: {}",
              entityForTest,
              env
          );
          return new EnvironmentLock<>(env, entityForTest, SUCCESS_HARD_LOCKED, "");
        } else if (!entityForTest.isRequiresHardLock() && lock(env)) {
          log.info(
              "Untested entity was found: {} and environment was locked: {}",
              entityForTest,
              env
          );
          return new EnvironmentLock<>(env, entityForTest, SUCCESS_LOCKED, "");
        } else {
          queues.add(entityForTest);
        }
      }
    }

    return new EnvironmentLock<>(FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS);
  }

}
