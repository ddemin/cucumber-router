package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.base.EntitiesQueues.ANY_ENVIRONMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

import com.github.ddemin.envrouter.base.EntitiesQueues;
import com.github.ddemin.envrouter.base.TestEntityWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EntitiesQueuesTests {

  private static final String ENV1 = "test1";
  private static final String ENV2 = "test2";

  private TestEntityWrapper<String> wrpEnv1P1;
  private TestEntityWrapper<String> wrpEnv1P2;
  private TestEntityWrapper<String> wrpEnv1P3;
  private TestEntityWrapper<String> wrpEnv2P1;
  private TestEntityWrapper<String> wrpEnv2P2;
  private TestEntityWrapper<String> wrpEnvAnyP1;
  private TestEntityWrapper<String> wrpEnvAnyP2;


  @BeforeClass
  public void setUp() {
    wrpEnv1P1 = new TestEntityWrapper<>("test", ENV1, 1);
    wrpEnv1P2 = new TestEntityWrapper<>("test", ENV1, 2);
    wrpEnv1P3 = new TestEntityWrapper<>("test", ENV1, 3);
    wrpEnv2P1 = new TestEntityWrapper<>("test", ENV2, 1);
    wrpEnv2P2 = new TestEntityWrapper<>("test", ENV2, 2);
    wrpEnvAnyP1 = new TestEntityWrapper<>("test", ANY_ENVIRONMENT, 1);
    wrpEnvAnyP2 = new TestEntityWrapper<>("test", ANY_ENVIRONMENT, 2);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForAdding() {
    EntitiesQueues<TestEntityWrapper<String>> queues = new EntitiesQueues<>();
    queues.add(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForAddingAll() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.addAll(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForGettingQueue() {
    EntitiesQueues queues = new EntitiesQueues();
    queues.getQueueFor(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForPollingFeature() {
    EntitiesQueues queues = new EntitiesQueues();
    queues.pollEntityFor(null);
  }

  public void checkAddingByOneAndRecoilByEnv() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnv1P3);
    queues.add(wrpEnv1P1);
    assertThat(queues.getQueueFor(ENV1), containsInAnyOrder(wrpEnv1P1, wrpEnv1P2, wrpEnv1P3));
  }

  public void checkAddingAllAndRecoilByEnv() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.addAll(Arrays.asList(wrpEnv2P2, wrpEnv2P1));
    assertThat(queues.getQueueFor(ENV2), containsInAnyOrder(wrpEnv2P1, wrpEnv2P2));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkTotalCount() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnv2P1);
    assertThat(queues.entitiesInAllQueues(), equalTo(2));
    queues.getQueueFor(ENV1).poll();
    assertThat(queues.entitiesInAllQueues(), equalTo(1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkUsageOfPriorityQueue() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
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
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollEntityFor("Unknown env"), equalTo(wrpEnvAnyP1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkFeaturePollingForUnknownEnv() {
    EntitiesQueues queues = new EntitiesQueues();
    queues.add(wrpEnv1P1);
    assertThat(queues.pollEntityFor("Unknown env"), nullValue());
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv1() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP2);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv2() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnvAnyP1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv3() {
    EntitiesQueues<TestEntityWrapper> queues = new EntitiesQueues<>();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollEntityFor(ENV1), equalTo(wrpEnv1P1));
  }

}
