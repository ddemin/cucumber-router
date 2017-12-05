package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.base.TestEntityWrapper.ANY_ENV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EnvsLocksControllerFindingTests extends UnitTestsBase {

  private static final String ENV1 = "test1";
  private static final String ENV2 = "test2";
  private static final String ENV_UNK = "unknown";

  private final EnvsLocksController<TestEntityWrapper<String>> controller;
  private TestEntityWrapper<String> wrpEnv1P1 = new TestEntityWrapper<>("demo", ENV1, 1);
  private TestEntityWrapper<String> wrpEnv2P1 = new TestEntityWrapper<>("demo", ENV2, 1);
  private TestEntityWrapper<String> wrpEnvAnyP1 = new TestEntityWrapper<>("demo", ANY_ENV, 1);
  private TestEntityWrapper<String> wrpEnvAnyP2 = new TestEntityWrapper<>("demo", ANY_ENV, 2);
  private TestEntityWrapper<String> wrpEnvUnk = new TestEntityWrapper<>("demo", ENV_UNK, 2);

  public EnvsLocksControllerFindingTests() {
    super();
    controller = new EnvsLocksController<>();
  }

  @BeforeMethod
  public void releaseEnvs() {
    controller.resetLockingOfAll();
  }

  public void checkPullingIfQueuesHasUndefined() throws IllegalAccessException {
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnvUnk);

    EnvironmentLock<TestEntityWrapper<String>> lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        lock.getLockStatus(),
        equalTo(LockStatus.FAILURE_UNDEFINED_ENV)
    );
    assertThat(
        lock.getTargetEntity(),
        equalTo(wrpEnvUnk)
    );
    assertThat(
        queues.entitiesInAllQueues(),
        is(0)
    );
  }

  public void checkPullingIfQueuesEmpty() throws IllegalAccessException {
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    EnvironmentLock<TestEntityWrapper<String>> lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        lock.getLockStatus(),
        equalTo(LockStatus.FAILURE_NO_TARGET_ENTITIES)
    );
  }

  public void checkPullingIfQueuesHasEntitiesForFirstEnv() throws IllegalAccessException {
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnvAnyP2);
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);

    EnvironmentLock<TestEntityWrapper<String>> lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        lock.getLockStatus(),
        equalTo(LockStatus.SUCCESS_LOCKED)
    );
    assertThat(
        "Must be entity for test1 env, because test1 - 1st environment and queues have entity for it",
        lock.getTargetEntity(),
        equalTo(wrpEnv1P1)
    );
    controller.release(lock.getEnvironment());

    lock = controller.findUntestedEntityAndLockEnv(queues);
    controller.release(lock.getEnvironment());
    assertThat(
        lock.getTargetEntity(),
        equalTo(wrpEnvAnyP1)
    );

    lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        lock.getTargetEntity(),
        equalTo(wrpEnvAnyP2)
    );
    controller.release(lock.getEnvironment());

    assertThat(
        queues.entitiesInAllQueues(),
        is(0)
    );
  }

  public void checkPullingIfQueuesHasEntitiesForSecondEnv() throws IllegalAccessException {
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnvAnyP2);
    queues.add(wrpEnv2P1);
    queues.add(wrpEnvAnyP1);

    EnvironmentLock<TestEntityWrapper<String>> lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        lock.getLockStatus(),
        equalTo(LockStatus.SUCCESS_LOCKED)
    );
    assertThat(
        "Must be entity for ANY environment, because queues don't contain entities for test1 strictly",
        lock.getTargetEntity(),
        equalTo(wrpEnvAnyP1)
    );
    controller.release(lock.getEnvironment());

    lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        "Must be entity for ANY environment, because queues don't contain entities strictly test1 strictly",
        lock.getTargetEntity(),
        equalTo(wrpEnvAnyP2)
    );
    controller.release(lock.getEnvironment());

    lock = controller.findUntestedEntityAndLockEnv(queues);
    assertThat(
        "Must be entity for test2 env, because queues don't contain entities for test1, but one for test2",
        lock.getTargetEntity(),
        equalTo(wrpEnv2P1)
    );
    controller.release(lock.getEnvironment());

    assertThat(
        queues.entitiesInAllQueues(),
        is(0)
    );
  }

  @Test(
      expectedExceptions = ConditionTimeoutException.class,
      expectedExceptionsMessageRegExp = ".*within 700 milliseconds.*",
      priority = 999
  )
  public void checkPullingTimeoutIfAllEnvsAreBusy() throws IllegalAccessException {
    changeRouterConfigConstants("LOCK_TIMEOUT_MS", 700);
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv2P1);

    controller.lock(controller.getByName(ENV2));
    controller.findUntestedEntityAndLockEnv(queues);
  }
}
