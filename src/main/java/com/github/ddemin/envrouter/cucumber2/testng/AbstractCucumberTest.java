package com.github.ddemin.envrouter.cucumber2.testng;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.base.EntitiesQueues;
import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.github.ddemin.envrouter.base.EnvironmentLocksController;
import com.github.ddemin.envrouter.base.EnvironmentsContext;
import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import com.github.ddemin.envrouter.cucumber2.FeaturesUtils;
import com.github.ddemin.testutil.io.FileSystemUtils;
import cucumber.api.testng.CucumberFeatureWrapper;
import cucumber.api.testng.TestNGCucumberRunner;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

@Slf4j
public abstract class AbstractCucumberTest implements ITest {

  private static final EnvironmentLocksController ENVS_CONTROLLER = new EnvironmentLocksController(
      () -> {
        try {
          log.debug("Find environments directiories in {}", RouterConfig.ENVS_DIRECTORY);
          return FileSystemUtils.getSubdirectories(
              AbstractCucumberTest.class
                  .getClassLoader()
                  .getResource(RouterConfig.ENVS_DIRECTORY)
                  .toURI()
          ).stream()
              .map(Environment::new)
              .collect(Collectors.toSet());
        } catch (URISyntaxException ex) {
          throw new RuntimeException(ex);
        }
      },
      RouterConfig.ENV_THREADS_MAX
  );
  private static final Map<
      Class<? extends AbstractCucumberTest>,
      EntitiesQueues<FeatureWrapper>
      > FEATURES_QUEUES = new HashMap<>();
  private final ThreadLocal<TestNGCucumberRunner> cukeRunner
      = ThreadLocal.withInitial(() -> new TestNGCucumberRunner(this.getClass()));
  private final ThreadLocal<EnvironmentLock<FeatureWrapper>> envLock
      = ThreadLocal.withInitial(() -> null);

  protected abstract void processFailedLocking(EnvironmentLock<FeatureWrapper> lock);

  @Override
  public String getTestName() {
    if (envLock.get() == null) {
      return "TestNG fixture";
    } else if (envLock.get().getTargetEntity() != null) {
      return envLock.get().getTargetEntity()
          .getEntity().getGherkinFeature().getFeature().getName();
    } else {
      return "Undefined feature. Global error occurred";
    }
  }

  @DataProvider(parallel = true)
  public Object[][] routerDataProvider() {
    return new Object[initFeaturesQueues()][];
  }

  /**
   * Try to lock some environment for untested feature during timeout.
   */
  @BeforeMethod(alwaysRun = true)
  public void lockEnvAndPrepareFeature() {
    log.info("Try to find untested feature and lock appropriate env...");
    envLock.set(
        await()
            .timeout(RouterConfig.LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(
                this::lockEnvAndSelectFeature,
                hasProperty(
                    "lockStatus",
                    not(
                        anyOf(
                            is(LockStatus.FAILURE_NO_AVAILABLE),
                            is(LockStatus.FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS)
                        )
                    )
                )
            )
    );
  }

  /**
   * Finish cucumber-jvm runner.
   */
  @AfterMethod(alwaysRun = true)
  public void finishCucumberRunner() {
    if (cukeRunner.get() != null) {
      log.debug("Finish current Cucumber runner");
      cukeRunner.get().finish();
    }
  }

  protected void runNextFeature() {
    log.debug("Try to run feature test. Lock info: {}", envLock.get());
    Environment env = envLock.get().getEnvironment();
    FeatureWrapper feature = envLock.get().getTargetEntity();
    LockStatus lockStatus = envLock.get().getLockStatus();
    switch (lockStatus) {
      case SUCCESS_LOCKED:
        try {
          EnvironmentsContext.setCurrent(env);
          cukeRunner.get().getFeatures();
          cukeRunner.get().runCucumber(feature.getEntity());
        } finally {
          ENVS_CONTROLLER.release(env);
        }
        break;
      default:
        log.warn("Lock unsuccessful. Process failure...");
        processFailedLocking(envLock.get());
    }
  }

  private EnvironmentLock<FeatureWrapper> lockEnvAndSelectFeature() {
    synchronized (AbstractCucumberTest.class) {
      try {
        EntitiesQueues<FeatureWrapper> ensQueues = getEnvsQueuesForThisClass();

        List<Entry<String, Queue<FeatureWrapper>>> queuesForUndefinedEnvs
            = FeaturesUtils.getQueuesForUndefinedEnvs(ensQueues, ENVS_CONTROLLER.getAll());
        Set<Environment> availableEnvs = ENVS_CONTROLLER.getAllAvailable();
        FeatureWrapper featureForTest = null;
        Environment envForTest = null;

        if (ensQueues.entitiesInAllQueues() <= 0) {
          log.error("No any features in queues");
          return new EnvironmentLock<>(LockStatus.FAILURE_NO_TARGET_ENTITIES);
        } else if (queuesForUndefinedEnvs.size() > 0) {
          FeatureWrapper featureWrapper = queuesForUndefinedEnvs.get(0).getValue().poll();
          log.warn("Feature for undefined found: {}", featureWrapper);
          return new EnvironmentLock<>(
              null,
              featureWrapper,
              LockStatus.FAILURE_UNDEFINED_ENV
          );
        } else if (availableEnvs.size() <= 0) {
          log.info("No any available environments");
          return new EnvironmentLock<>(LockStatus.FAILURE_NO_AVAILABLE);
        } else {
          log.debug("Search untested features for available environments...");
          for (Environment env : availableEnvs) {
            featureForTest = ensQueues.pollEntityFor(env.getName());
            if (featureForTest != null) {
              envForTest = env;
              break;
            }
          }
        }

        if (featureForTest != null && ENVS_CONTROLLER.lock(envForTest)) {
          log.info(
              "Untested feature is found: {} and environment is locked: {}",
              featureForTest,
              envForTest
          );
          return new EnvironmentLock<>(envForTest, featureForTest, LockStatus.SUCCESS_LOCKED);
        } else {
          log.error("Something goes wrong");
          return new EnvironmentLock<>(LockStatus.FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS);
        }
      } catch (NullPointerException ex) {
        ex.printStackTrace();
      }
    }
    return null;
  }

  private int initFeaturesQueues() {
    EntitiesQueues<FeatureWrapper> ensQueues = getEnvsQueuesForThisClass();
    ensQueues.addAll(
        FeaturesUtils.wrapFeatures(
            Arrays.stream(cukeRunner.get().provideFeatures())
                .flatMap(
                    objs -> Arrays.stream(objs)
                        .map(obj -> ((CucumberFeatureWrapper) obj).getCucumberFeature())
                )
                .distinct()
                .collect(Collectors.toList())
        )
    );
    log.info("Save all features to queues. Processed {}", ensQueues.entitiesInAllQueues());
    return ensQueues.entitiesInAllQueues();
  }

  private EntitiesQueues<FeatureWrapper> getEnvsQueuesForThisClass() {
    return FEATURES_QUEUES.computeIfAbsent(this.getClass(), clz -> new EntitiesQueues<>());
  }

}
