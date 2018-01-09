package com.github.ddemin.envrouter.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Created by Dmitrii Demin on 19.09.2017.
 */
@AllArgsConstructor
@Getter
public class EnvironmentLock<T> {

  private Environment environment;
  private T targetEntity;
  private LockStatus lockStatus;

  private String statusMessage;

  public EnvironmentLock(@NonNull LockStatus lockStatus) {
    this.lockStatus = lockStatus;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public enum LockStatus {
    SUCCESS_LOCKED,
    SUCCESS_HARD_LOCKED,
    FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS,
    FAILURE_NO_AVAILABLE,
    FAILURE_NO_TARGET_ENTITIES,
    FAILURE_UNDEFINED_ENV,
    FAILURE_TIMEOUT
  }

}
