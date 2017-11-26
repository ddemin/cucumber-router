package com.github.ddemin.envrouter.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Dmitrii Demin on 19.09.2017.
 */
@AllArgsConstructor
@Getter
public class EnvironmentLock<T> {

  private Environment environment;
  private T targetEntity;
  private LockStatus lockStatus;

  public EnvironmentLock(LockStatus lockStatus) {
    this.lockStatus = lockStatus;
  }

  public enum LockStatus {
    SUCCESS_LOCKED,
    FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS,
    FAILURE_NO_AVAILABLE,
    FAILURE_NO_TARGET_ENTITIES,
    FAILURE_UNDEFINED_ENV
  }


}
