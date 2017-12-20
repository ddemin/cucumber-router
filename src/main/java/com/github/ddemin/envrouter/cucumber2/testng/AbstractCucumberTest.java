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
import org.testng.ITest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

@Slf4j
public abstract class AbstractCucumberTest<T extends TestEntityWrapper> implements ITest {

  // TODO Prettify
  private static final EnvsLocksController<TestEntityWrapper> CONTROLLER = new EnvsLocksController<>();
  private static final Map<Class<? extends AbstractCucumberTest>, TestEntitiesQueues<? extends TestEntityWrapper>>
      QUEUES = new HashMap<>();

  final ThreadLocal<TestNGCucumberRunner> tlCukeRunner
      = ThreadLocal.withInitial(() -> new TestNGCucumberRunner(this.getClass()));
  private final ThreadLocal<EnvironmentLock<T>> tlEnvLock = ThreadLocal.withInitial(() -> null);

  protected abstract void processFailedLocking(EnvironmentLock<T> lock);

  abstract List<T> wrapEntities();

  abstract void runCucumberEntity(T cucumberEntityWrapper);

  {
    log.info("Transfer tags from property to cucumber runtime options...");
    List<String> tagGroups;
    if (CukeConfig.TAGS != null) {
      tagGroups = Splitter.on(';').splitToList(CukeConfig.TAGS);
      log.info("Tags: " + tagGroups);

      CucumberOptions cucumberOptions = getClass().getAnnotation(CucumberOptions.class);
      changeAnnotationValue(cucumberOptions, "tags", tagGroups.toArray(new String[tagGroups.size()]));
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
    tlEnvLock.set(
        (EnvironmentLock<T>) CONTROLLER.findUntestedEntityAndLockEnv(
            (TestEntitiesQueues<TestEntityWrapper>) getEnvsQueuesForThisClass())
    );
  }

  /**
   * Finish cucumber-jvm runner.
   */
  @AfterMethod(alwaysRun = true)
  public void finishCucumberRunner() {
    if (tlCukeRunner.get() != null) {
      log.debug("Finish current Cucumber runner");
      tlCukeRunner.get().finish();
    }
  }

  protected void runNextCukeEntity() {
    log.debug("Try to run feature demo. Lock info: {}", tlEnvLock.get());
    Environment env = tlEnvLock.get().getEnvironment();
    LockStatus lockStatus = tlEnvLock.get().getLockStatus();
    switch (lockStatus) {
      case SUCCESS_LOCKED:
        try {
          EnvironmentsUtils.setCurrent(env);
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
