package com.github.ddemin.envrouter.base;

import static java.lang.String.format;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 30.11.2017.
 */
@Getter
@Slf4j(topic = "wrapper")
@AllArgsConstructor
public class TestEntityWrapper<T> {

  @NonNull
  private T entity;
  @NonNull
  private String requiredEnvironmentName;
  private int priority;

  @Override
  public String toString() {
    return format(
        "%s (Env: %s, Priority: %d)",
        getEntity(),
        getRequiredEnvironmentName(),
        getPriority()
    );
  }

}
