package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.base.TestEntityWrapper.ANY_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestEntitiesQueuesTests {

  private static final String ENV1 = "test1";
  private static final String ENV2 = "test2";
  private static final String ENV_UNK = "test2Unknown";

  private TestEntityWrapper<String> wrpEnv1P1;
  private TestEntityWrapper<String> wrpEnv1P2;
  private TestEntityWrapper<String> wrpEnv1P3;
  private TestEntityWrapper<String> wrpEnv2P1;
  private TestEntityWrapper<String> wrpEnv2P2;
  private TestEntityWrapper<String> wrpEnvAnyP1;
  private TestEntityWrapper<String> wrpEnvAnyP2;
  private TestEntityWrapper<String> wrpEnvUnk;


  @BeforeClass
  public void setUp() {
    wrpEnv1P1 = new TestEntityWrapper<>("demo", ENV1, 1);
    wrpEnv1P2 = new TestEntityWrapper<>("demo", ENV1, 2);
    wrpEnv1P3 = new TestEntityWrapper<>("demo", ENV1, 3);
    wrpEnv2P1 = new TestEntityWrapper<>("demo", ENV2, 1);
    wrpEnv2P2 = new TestEntityWrapper<>("demo", ENV2, 2);
    wrpEnvAnyP1 = new TestEntityWrapper<>("demo", ANY_ENV, 1);
    wrpEnvAnyP2 = new TestEntityWrapper<>("demo", ANY_ENV, 2);
    wrpEnvUnk = new TestEntityWrapper<>("demo", ENV_UNK, 2);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "entity")
  public void catchNpeForAdding() {
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    queues.add(null);
  }

  @Test
  public void getQueuesForUndefinedEnv() {
    TestEntitiesQueues<TestEntityWrapper<String>> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnvUnk);
    queues.add(wrpEnvAnyP1);
    queues.add(wrpEnv2P1);
    List<Entry<String, Queue<TestEntityWrapper<String>>>> queuesForUndefinedEnvs =  queues.getQueuesForUndefinedEnvs(
        new HashSet<>(
            Arrays.asList(
                new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV2))
            )
        )
    );
    assertThat(queuesForUndefinedEnvs, hasSize(1));
    assertThat(queuesForUndefinedEnvs.get(0).getKey(), equalTo(ENV_UNK));
    assertThat(queuesForUndefinedEnvs.get(0).getValue().poll(), equalTo(wrpEnvUnk));

    queuesForUndefinedEnvs =  queues.getQueuesForUndefinedEnvs(
        new HashSet<>(
            Arrays.asList(
                new Environment(Paths.get("src/test/resources", RouterConfig.ENVS_DIRECTORY, ENV2))
            )
        )
    );
    assertThat(queuesForUndefinedEnvs, hasSize(0));
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "definedEnvs")
  public void catchNpeForGettingForUndefEnv() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.getQueuesForUndefinedEnvs(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "entities")
  public void catchNpeForAddingAll() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.addAll(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "definedEnv")
  public void catchNpeForGettingQueue() {
    TestEntitiesQueues queues = new TestEntitiesQueues();
    queues.getQueueFor(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "envName")
  public void catchNpeForPollingFeature() {
    TestEntitiesQueues queues = new TestEntitiesQueues();
    queues.pollEntityFor(null);
  }

  public void checkAddingByOneAndRecoilByEnv() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnv1P3);
    queues.add(wrpEnv1P1);
    assertThat(queues.getQueueFor(ENV1), containsInAnyOrder(wrpEnv1P1, wrpEnv1P2, wrpEnv1P3));
  }

  public void checkAddingAllAndRecoilByEnv() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.addAll(Arrays.asList(wrpEnv2P2, wrpEnv2P1));
    assertThat(queues.getQueueFor(ENV2), containsInAnyOrder(wrpEnv2P1, wrpEnv2P2));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkTotalCount() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnv2P1);
    assertThat(queues.entitiesInAllQueues(), equalTo(2));
    queues.getQueueFor(ENV1).poll();
    assertThat(queues.entitiesInAllQueues(), equalTo(1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkUsageOfPriorityQueue() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnv1P3);
    queues.add(wrpEnv1P1);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P1));
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P2));
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P3));
    assertThat(queues.getQueueFor(ENV1), nullValue());
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkFeaturePollingForAnyEnv() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollEntityFor("Unknown env"), equalTo(wrpEnvAnyP1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkFeaturePollingForUnknownEnv() {
    TestEntitiesQueues queues = new TestEntitiesQueues();
    queues.add(wrpEnv1P1);
    assertThat(queues.pollEntityFor("Unknown env"), nullValue());
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv1() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP2);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv2() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnvAnyP1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv3() {
    TestEntitiesQueues<TestEntityWrapper> queues = new TestEntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P1));
  }

}
