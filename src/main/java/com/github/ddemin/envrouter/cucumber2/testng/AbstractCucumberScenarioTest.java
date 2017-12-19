package com.github.ddemin.envrouter.cucumber2.testng;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.github.ddemin.envrouter.base.EnvironmentsUtils;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.cucumber2.ScenarioWrapper;
import com.github.ddemin.envrouter.cucumber2.ScenariosUtils;
import cucumber.api.testng.CucumberFeatureWrapper;
import cucumber.api.testng.PickleEventWrapper;
import cucumber.api.testng.TestNGCucumberRunner;
import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

@Slf4j
public abstract class AbstractCucumberScenarioTest implements ITest {

  private static final EnvsLocksController<ScenarioWrapper> CONTROLLER = new EnvsLocksController<>();
  private static final Map<Class<? extends AbstractCucumberScenarioTest>, TestEntitiesQueues<ScenarioWrapper>> QUEUES
      = new HashMap<>();

  private final ThreadLocal<EnvironmentLock<ScenarioWrapper>> envLock = ThreadLocal
      .withInitial(() -> null);
  private final ThreadLocal<TestNGCucumberRunner> cukeRunner
      = ThreadLocal.withInitial(() -> new TestNGCucumberRunner(this.getClass()));

  protected abstract void processFailedLocking(EnvironmentLock<ScenarioWrapper> lock);

  @Override
  public String getTestName() {
    if (envLock.get() == null) {
      return "TestNG fixture";
    } else if (envLock.get().getTargetEntity() != null) {
      return envLock.get().getTargetEntity().getEntity().pickle.getName();
    } else {
      return "Undefined scenario. Global error occurred";
    }
  }

  @DataProvider(parallel = true)
  public Object[][] routerDataProvider() {
    return new Object[initScenariosQueue()][];
  }

  /**
   * Try to lock some environment for untested scenario during timeout.
   */
  @BeforeMethod(alwaysRun = true)
  public void lockEnvAndPrepareScenario() {
    log.info("Try to find untested scenario and lock appropriate env...");
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

  protected void runNextScenario() {
    log.debug("Try to run scenario test. Lock info: {}", envLock.get());
    Environment env = envLock.get().getEnvironment();
    ScenarioWrapper scenario = envLock.get().getTargetEntity();
    LockStatus lockStatus = envLock.get().getLockStatus();
    switch (lockStatus) {
      case SUCCESS_LOCKED:
        try {
          EnvironmentsUtils.setCurrent(env);
          cukeRunner.get().getFeatures();
          cukeRunner.get().runScenario(scenario.getEntity());
        } catch (Throwable throwable) {
          throw new RuntimeException(throwable);
        } finally {
          CONTROLLER.release(env);
        }
        break;
      default:
        log.warn("Lock unsuccessful. Process failure...");
        processFailedLocking(envLock.get());
    }
  }

  private int initScenariosQueue() {
    Map<CucumberFeature, List<PickleEvent>> scenariosMap = Arrays.stream(cukeRunner.get().provideScenarios())
        .map(objs ->
            new Pair<>(
                ((CucumberFeatureWrapper) objs[1]).getCucumberFeature(),
                ((PickleEventWrapper) objs[0]).getPickleEvent()
            )
        )
        .collect(
            Collectors.groupingBy(
                Pair::getKey,
                mapping(Pair::getValue, toList())
            )
        );
    TestEntitiesQueues<ScenarioWrapper> envQueues = getEnvsQueuesForThisClass();
    envQueues.addAll(ScenariosUtils.wrapScenarios(scenariosMap));
    log.info("Save all scenarios to queues. Processed {}", envQueues.entitiesInAllQueues());
    return envQueues.entitiesInAllQueues();
  }

  private TestEntitiesQueues<ScenarioWrapper> getEnvsQueuesForThisClass() {
    return QUEUES.computeIfAbsent(this.getClass(), clz -> new TestEntitiesQueues<>());
  }

}
