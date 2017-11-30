package com.github.ddemin.envrouter.base;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 19.09.2017.
 */
@Slf4j
public class EntitiesQueues<T extends TestEntityWrapper> {

  public static final String ANY_ENVIRONMENT = "any";

  private Map<String, Queue<T>> entitysQueuesForEnvs = new HashMap<>();

  /**
   * See #add.
   *
   * @param entitys wrapped entitys
   */
  public void addAll(@NonNull List<T> entitys) {
    entitys.forEach(this::add);
  }

  /**
   * Add entity to appropriate queue by environment name that required for entity.
   *
   * @param entity wrapped cucumber entity
   */
  public void add(@NonNull T entity) {
    String requiredEnv = entity.getRequiredEnvironmentName();
    if (entitysQueuesForEnvs.get(requiredEnv) == null) {
      entitysQueuesForEnvs.put(
          requiredEnv,
          new PriorityQueue<>(Comparator.comparingInt(T::getPriority))
      );
    }
    entitysQueuesForEnvs.get(requiredEnv).add(entity);
  }

  /**
   * Poll entity from queue that assign to environment name.
   *
   * @param envName environment
   * @return extract entity from environment queue
   */
  public T pollEntityFor(@NonNull String envName) {
    log.debug("Try to poll entity for environment: {}", envName);

    T chosenEntity;
    Queue<T> queueForEnv = getQueueFor(envName);
    Queue<T> queueForAnyEnv = getQueueFor(ANY_ENVIRONMENT);

    if (queueForEnv == null && queueForAnyEnv == null) {
      log.warn("No entity found for env-s: ANY & {}", envName);
      chosenEntity = null;
    } else if (queueForEnv == null) {
      log.debug("Try to poll entity from queue for ANY environment");
      chosenEntity = queueForAnyEnv.poll();
    } else if (queueForAnyEnv == null) {
      log.debug("Try to poll entity from queue for environment {}", envName);
      chosenEntity = queueForEnv.poll();
    } else {
      chosenEntity =
          queueForEnv.peek().getPriority() <= queueForAnyEnv.peek().getPriority()
              ? queueForEnv.poll()
              : queueForAnyEnv.poll();
    }

    log.debug("Entity was pulled from queue: {}", chosenEntity);
    return chosenEntity;
  }

  public Map<String, Queue<T>> getQueuesMap() {
    return entitysQueuesForEnvs;
  }

  /**
   * Get total count of entitys in queues.
   *
   * @return total count of found entity files
   */
  public int entitiesInAllQueues() {
    return entitysQueuesForEnvs.values().stream()
        .map(Queue::size)
        .reduce(0, Integer::sum);
  }

  /**
   * Get queue for environment (by name).
   *
   * @param definedEnv name of environment that exists
   * @return queue for environment
   */
  public Queue<T> getQueueFor(@NonNull String definedEnv) {
    Entry<String, Queue<T>> entry = entitysQueuesForEnvs.entrySet().stream()
        .filter(it -> definedEnv.startsWith(it.getKey()) && it.getValue().size() > 0)
        .findFirst()
        .orElse(null);
    return entry == null ? null : entry.getValue();
  }

}
