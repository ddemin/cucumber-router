package com.github.ddemin.envrouter.base;

import static java.lang.String.format;

import com.github.ddemin.envrouter.RouterConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 30.11.2017.
 */
@Getter
@Slf4j(topic = "wrapper")
public class TestEntityWrapper<T> {

  public static final String ANY_ENV = "any";

  private T entity;
  private String requiredEnvironmentName;
  private int priority;

  /**
   * Creates wrapper for test entity.
   *
   * @param entity entity
   * @param requiredEnvironmentName name of environment that required for this entity
   * @param priority entity's priority. Lower - more chances to be tested first
   */
  @SuppressFBWarnings
  public TestEntityWrapper(@NonNull T entity, String requiredEnvironmentName, int priority) {
    this.entity = entity;

    if (RouterConfig.ENV_FORCED != null) {
      this.requiredEnvironmentName = RouterConfig.ENV_FORCED.toLowerCase();
    } else {
      this.requiredEnvironmentName = requiredEnvironmentName == null
          ? chooseAnyOrDefaultEnv().toLowerCase()
          : requiredEnvironmentName.toLowerCase();
    }
    this.priority = priority;
  }

  @Override
  public String toString() {
    return format(
        "%s (Env: %s, Priority: %d)",
        getEntity(),
        getRequiredEnvironmentName(),
        getPriority()
    );
  }

  private String chooseAnyOrDefaultEnv() {
    return RouterConfig.ENV_DEFAULT == null ? ANY_ENV : RouterConfig.ENV_DEFAULT;
  }

}
