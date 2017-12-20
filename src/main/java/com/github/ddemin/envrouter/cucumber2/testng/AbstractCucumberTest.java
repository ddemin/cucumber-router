package com.github.ddemin.envrouter.cucumber2.testng;

import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.github.ddemin.envrouter.base.EnvironmentsUtils;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import cucumber.api.testng.TestNGCucumberRunner;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

@Slf4j
public abstract class AbstractCucumberTest<T extends TestEntityWrapper> implements ITest {

  // TODO Prettify
  private static final EnvsLocksController<TestEntityWrapper> CONTROLLER
      = new EnvsLocksController<>();
  private static final Map<
      Class<? extends AbstractCucumberTest>,
      TestEntitiesQueues<? extends TestEntityWrapper>
      >
      QUEUES = new HashMap<>();

  final ThreadLocal<TestNGCucumberRunner> cukeRunner
      = ThreadLocal.withInitial(() -> new TestNGCucumberRunner(this.getClass()));
  private final ThreadLocal<EnvironmentLock<T>> envLock = ThreadLocal.withInitial(() -> null);

  protected abstract void processFailedLocking(EnvironmentLock<T> lock);

  abstract int initQueues();

  abstract void runCucumberEntity(T cucumberEntityWrapper);

  @Override
  public String getTestName() {
    if (envLock.get() == null) {
      return "TestNG fixture";
    } else if (envLock.get().getTargetEntity() != null) {
      return envLock.get().getTargetEntity().getName();
    } else {
      return "Undefined entity. Global error occurred";
    }
  }

  @DataProvider(parallel = true)
  public Object[][] routerDataProvider() {
    return new Object[initQueues()][];
  }

  /**
   * Try to lock some environment for untested feature during timeout.
   */
  @BeforeMethod(alwaysRun = true)
  public void lockEnvAndPrepareFeature() {
    log.info("Try to find untested feature and lock appropriate env...");
    envLock.set(
        (EnvironmentLock<T>) CONTROLLER.findUntestedEntityAndLockEnv(
            (TestEntitiesQueues<TestEntityWrapper>) getEnvsQueuesForThisClass())
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

  protected void runNextCukeEntity() {
    log.debug("Try to run feature demo. Lock info: {}", envLock.get());
    Environment env = envLock.get().getEnvironment();
    LockStatus lockStatus = envLock.get().getLockStatus();
    switch (lockStatus) {
      case SUCCESS_LOCKED:
        try {
          EnvironmentsUtils.setCurrent(env);
          cukeRunner.get().getFeatures();
          runCucumberEntity(envLock.get().getTargetEntity());
        } finally {
          CONTROLLER.release(env);
        }
        break;
      default:
        log.warn("Lock unsuccessful. Process failure...");
        processFailedLocking(envLock.get());
    }
  }

  TestEntitiesQueues<T> getEnvsQueuesForThisClass() {
    return (TestEntitiesQueues<T>) QUEUES.computeIfAbsent(
        this.getClass(),
        clz -> new TestEntitiesQueues<>()
    );
  }

}
