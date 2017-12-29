package com.github.ddemin.envrouter.cucumber2.testng;

import static com.github.ddemin.envrouter.util.ReflectionUtils.changeAnnotationValue;

import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.github.ddemin.envrouter.base.EnvironmentsUtils;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import com.github.ddemin.envrouter.cucumber2.CukeConfig;
import com.google.common.base.Splitter;
import cucumber.api.CucumberOptions;
import cucumber.api.testng.TestNGCucumberRunner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.testng.ITest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

@Slf4j
public abstract class AbstractCucumberTest<T extends TestEntityWrapper> implements ITest {

  // TODO Prettify
  private static final EnvsLocksController<TestEntityWrapper> CONTROLLER = new EnvsLocksController<>();
  private static final Map<Class<? extends AbstractCucumberTest>, TestEntitiesQueues<? extends TestEntityWrapper>>
      QUEUES = new HashMap<>();
  private static final String LOGBACK_MDC_KEY = "mdc";

  final ThreadLocal<TestNGCucumberRunner> tlCukeRunner
      = ThreadLocal.withInitial(() -> new TestNGCucumberRunner(this.getClass()));
  private final ThreadLocal<EnvironmentLock<T>> tlEnvLock = ThreadLocal.withInitial(() -> null);
  private boolean annotationIsUpdated = false;

  protected abstract void processFailedLocking(EnvironmentLock<T> lock);

  abstract List<T> wrapEntities();

  abstract void runCucumberEntity(T cucumberEntityWrapper) throws Throwable;

  {
    if (!annotationIsUpdated) {
      log.debug("Transfer tags from property to cucumber runtime options...");
      List<String> tagGroups;
      if (CukeConfig.TAGS != null) {
        tagGroups = Splitter.on(';').splitToList(CukeConfig.TAGS);
        log.info("Tags: " + tagGroups);

        CucumberOptions cucumberOptions = getClass().getAnnotation(CucumberOptions.class);
        changeAnnotationValue(cucumberOptions, "tags", tagGroups.toArray(new String[tagGroups.size()]));
        annotationIsUpdated = true;
      }
    }
  }

  @Override
  public String getTestName() {
    if (tlEnvLock.get() == null) {
      return "TestNG fixture";
    } else if (tlEnvLock.get().getTargetEntity() != null) {
      return tlEnvLock.get().getTargetEntity().getName();
    } else {
      return "Undefined entity. Global error occurred";
    }
  }

  /**
   * TestNG data provider for creation of test classes that represent cucumber entities.
   * @return array of dummy objects
   */
  @DataProvider(parallel = true)
  public Object[][] routerDataProvider() {
    MDC.put(LOGBACK_MDC_KEY, "cucumber-router");
    try {
      return new Object[initQueues()][];
    } finally {
      MDC.remove(LOGBACK_MDC_KEY);
    }
  }


  /**
   * Try to lock some environment for untested cuke entity during timeout.
   */
  @BeforeClass(alwaysRun = true)
  public void lockEnvAndPrepareEntity() {
    MDC.put(LOGBACK_MDC_KEY, "cucumber-router");
    log.info("Try to find untested entity and lock appropriate env...");
    tlEnvLock.set(
        (EnvironmentLock<T>) CONTROLLER.findUntestedEntityAndLockEnv(
            (TestEntitiesQueues<TestEntityWrapper>) getEnvsQueuesForThisClass())
    );
    if (tlEnvLock.get().getTargetEntity() != null) {
      MDC.put(LOGBACK_MDC_KEY, tlEnvLock.get().getTargetEntity().getName());
    }
  }

  /**
   * Finish cucumber-jvm runner.
   */
  @AfterClass(alwaysRun = true)
  public void finishCucumberRunner() {
    if (tlCukeRunner.get() != null) {
      log.debug("Finish current Cucumber runner");
      tlCukeRunner.get().finish();
    }
    MDC.remove(LOGBACK_MDC_KEY);
  }

  protected void testCucumberEntity() throws Throwable {
    log.debug("Try to run cucumber entity. Lock info: {}", tlEnvLock.get());
    Environment env = tlEnvLock.get().getEnvironment();
    LockStatus lockStatus = tlEnvLock.get().getLockStatus();
    switch (lockStatus) {
      case SUCCESS_LOCKED:
        try {
          EnvironmentsUtils.setCurrent(env);
          // For init purposes
          tlCukeRunner.get().getFeatures();
          runCucumberEntity(tlEnvLock.get().getTargetEntity());
        } finally {
          CONTROLLER.release(env);
        }
        break;
      default:
        log.warn("Lock unsuccessful. Process failure...");
        processFailedLocking(tlEnvLock.get());
    }
  }

  private TestEntitiesQueues<T> getEnvsQueuesForThisClass() {
    return (TestEntitiesQueues<T>) QUEUES.computeIfAbsent(
        this.getClass(),
        clz -> new TestEntitiesQueues<>()
    );
  }

  private int initQueues() {
    TestEntitiesQueues<T> envQueues = getEnvsQueuesForThisClass();
    envQueues.addAll(wrapEntities());
    log.info("Save all scenarios to queues. Processed {}", envQueues.entitiesInAllQueues());
    return envQueues.entitiesInAllQueues();
  }

}
