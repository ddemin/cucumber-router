package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.cucumber2.FeaturesQueues.ANY_ENVIRONMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import com.github.ddemin.envrouter.cucumber2.FeaturesQueues;
import cucumber.runtime.model.CucumberFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FeaturesQueuesTests {

  static final String ENV1 = "test1";
  static final String ENV2 = "test2";

  @Mock
  CucumberFeature feature;
  FeatureWrapper wrpEnv1P1;
  FeatureWrapper wrpEnv1P2;
  FeatureWrapper wrpEnv1P3;
  FeatureWrapper wrpEnv2P1;
  FeatureWrapper wrpEnv2P2;
  FeatureWrapper wrpEnvAnyP1;
  FeatureWrapper wrpEnvAnyP2;


  @BeforeClass
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(feature.getUri()).thenReturn("test uri");
    wrpEnv1P1 = new FeatureWrapper(feature, ENV1, 1);
    wrpEnv1P2 = new FeatureWrapper(feature, ENV1, 2);
    wrpEnv1P3 = new FeatureWrapper(feature, ENV1, 3);
    wrpEnv2P1 = new FeatureWrapper(feature, ENV2, 1);
    wrpEnv2P2 = new FeatureWrapper(feature, ENV2, 2);
    wrpEnvAnyP1 = new FeatureWrapper(feature, ANY_ENVIRONMENT, 1);
    wrpEnvAnyP2 = new FeatureWrapper(feature, ANY_ENVIRONMENT, 2);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForAdding() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForAddingAll() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.addAll(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForGettingQueue() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.getQueueFor(null);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForPollingFeature() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.pollFeatureFor(null);
  }

  public void checkAddingByOneAndRecoilByEnv() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnv1P3);
    queues.add(wrpEnv1P1);
    assertThat(queues.getQueueFor(ENV1), containsInAnyOrder(wrpEnv1P1, wrpEnv1P2, wrpEnv1P3));
  }

  public void checkAddingAllAndRecoilByEnv() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.addAll(Arrays.asList(wrpEnv2P2, wrpEnv2P1));
    assertThat(queues.getQueueFor(ENV2), containsInAnyOrder(wrpEnv2P1, wrpEnv2P2));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkTotalCount() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnv2P1);
    assertThat(queues.featuresInAllQueues(), equalTo(2));
    queues.getQueueFor(ENV1).poll();
    assertThat(queues.featuresInAllQueues(), equalTo(1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkUsageOfPriorityQueue() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnv1P3);
    queues.add(wrpEnv1P1);
    assertThat(queues.pollFeatureFor(ENV1), equalTo(wrpEnv1P1));
    assertThat(queues.pollFeatureFor(ENV1), equalTo(wrpEnv1P2));
    assertThat(queues.pollFeatureFor(ENV1), equalTo(wrpEnv1P3));
    assertThat(queues.getQueueFor(ENV1), nullValue());
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkFeaturePollingForAnyEnv() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollFeatureFor("Unknown env"), equalTo(wrpEnvAnyP1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkFeaturePollingForUnknownEnv() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P1);
    assertThat(queues.pollFeatureFor("Unknown env"), nullValue());
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv1() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP2);
    assertThat(queues.pollFeatureFor(ENV1), equalTo(wrpEnv1P1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv2() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P2);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollFeatureFor(ENV1), equalTo(wrpEnvAnyP1));
  }

  @Test(dependsOnMethods = {"checkAddingByOneAndRecoilByEnv"})
  public void checkPriorityResolvingBetweenEnvAndAnyEnv3() {
    FeaturesQueues queues = new FeaturesQueues();
    queues.add(wrpEnv1P1);
    queues.add(wrpEnvAnyP1);
    assertThat(queues.pollFeatureFor(ENV1), equalTo(wrpEnv1P1));
  }

}
