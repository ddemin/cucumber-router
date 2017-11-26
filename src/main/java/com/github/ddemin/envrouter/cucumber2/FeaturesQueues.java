package com.github.ddemin.envrouter.cucumber2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 19.09.2017.
 */
@Slf4j
public class FeaturesQueues {

  static final String ANY_ENVIRONMENT = "any";

  private Map<String, Queue<FeatureWrapper>> featuresQueuesForEnvs = new HashMap<>();

  /**
   * See #add.
   *
   * @param features wrapped features
   */
  public void addAll(List<FeatureWrapper> features) {
    features.forEach(this::add);
  }

  /**
   * Add feature to appropriate queue by environment name that required for feature.
   *
   * @param feature wrapped cucumber feature
   */
  public void add(FeatureWrapper feature) {
    String requiredEnv = feature.getRequiredEnvironmentName();
    if (featuresQueuesForEnvs.get(requiredEnv) == null) {
      featuresQueuesForEnvs.put(
          requiredEnv,
          new PriorityQueue<>(Comparator.comparingInt(FeatureWrapper::getPriority).reversed())
      );
    }
    featuresQueuesForEnvs.get(requiredEnv).add(feature);
  }

  /**
   * Poll feature from queue that assign to environment name.
   *
   * @param envName environment
   * @return extract feature from environment queue
   */
  public FeatureWrapper pollFeatureFor(String envName) {
    FeatureWrapper chosenFeature;
    Queue<FeatureWrapper> queueForEnv = getQueueFor(envName);
    Queue<FeatureWrapper> queueForAnyEnv = getQueueFor(ANY_ENVIRONMENT);

    if (queueForEnv == null && queueForAnyEnv == null) {
      log.warn("No feature found for env-s: ANY & {}", envName);
      chosenFeature = null;
    } else if (queueForEnv == null) {
      chosenFeature = queueForAnyEnv.poll();
    } else if (queueForAnyEnv == null) {
      chosenFeature = queueForEnv.poll();
    } else {
      chosenFeature =
          queueForEnv.peek().getPriority() >= queueForAnyEnv.peek().getPriority()
              ? queueForEnv.poll()
              : queueForAnyEnv.poll();
    }

    return chosenFeature;
  }

  /**
   * Get all queues.
   *
   * @return map of environments and its feature queue
   */
  public Map<String, Queue<FeatureWrapper>> getQueuesMap() {
    return featuresQueuesForEnvs;
  }

  /**
   * Get total count of features in queues.
   *
   * @return total count of found feature files
   */
  public int featuresInAllQueues() {
    return featuresQueuesForEnvs.values().stream()
        .map(Queue::size)
        .reduce(0, Integer::sum);
  }

  /**
   * Get queue for environment (by name).
   *
   * @param definedEnv name of environment that exists
   * @return queue for environment
   */
  public Queue<FeatureWrapper> getQueueFor(String definedEnv) {
    Entry<String, Queue<FeatureWrapper>> entry = featuresQueuesForEnvs.entrySet().stream()
        .filter(it -> definedEnv.startsWith(it.getKey()) && it.getValue().size() > 0)
        .findFirst()
        .orElse(null);
    return entry == null ? null : entry.getValue();
  }

}
