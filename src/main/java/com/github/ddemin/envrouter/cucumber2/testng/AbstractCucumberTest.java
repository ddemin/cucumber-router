package com.github.ddemin.envrouter.cucumber2.testng;

import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.github.ddemin.envrouter.base.EnvironmentsUtils;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import com.github.ddemin.envrouter.cucumber2.FeaturesUtils;
import cucumber.api.testng.CucumberFeatureWrapper;
import cucumber.api.testng.TestNGCucumberRunner;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

@Slf4j
public abstract class AbstractCucumberTest implements ITest {

  private static final EnvsLocksController<FeatureWrapper> CONTROLLER = new EnvsLocksController<>();
  private static final Map<Class<? extends AbstractCucumberTest>, TestEntitiesQueues<FeatureWrapper>> QUEUES
      = new HashMap<>();

  private final ThreadLocal<EnvironmentLock<FeatureWrapper>> envLock = ThreadLocal.withInitial(() -> null);
  private final ThreadLocal<TestNGCucumberRunner> cukeRunner
      = ThreadLocal.withInitial(() -> new TestNGCucumberRunner(this.getClass()));

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
    envLock.set(CONTROLLER.findUntestedEntityAndLockEnv(getEnvsQueuesForThisClass()));
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
    log.debug("Try to run feature demo. Lock info: {}", envLock.get());
    Environment env = envLock.get().getEnvironment();
    FeatureWrapper feature = envLock.get().getTargetEntity();
    LockStatus lockStatus = envLock.get().getLockStatus();
    switch (lockStatus) {
      case SUCCESS_LOCKED:
        try {
          EnvironmentsUtils.setCurrent(env);
          cukeRunner.get().getFeatures();
          cukeRunner.get().runCucumber(feature.getEntity());
        } finally {
          CONTROLLER.release(env);
        }
        break;
      default:
        log.warn("Lock unsuccessful. Process failure...");
        processFailedLocking(envLock.get());
    }
  }

  private int initFeaturesQueues() {
    TestEntitiesQueues<FeatureWrapper> envQueues = getEnvsQueuesForThisClass();
    envQueues.addAll(
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
    log.info("Save all features to queues. Processed {}", envQueues.entitiesInAllQueues());
    return envQueues.entitiesInAllQueues();
  }

  private TestEntitiesQueues<FeatureWrapper> getEnvsQueuesForThisClass() {
    return QUEUES.computeIfAbsent(this.getClass(), clz -> new TestEntitiesQueues<>());
  }

}
