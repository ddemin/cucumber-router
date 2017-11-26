package com.github.ddemin.envrouter.base;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvironmentsContext {

  private static final ThreadLocal<Environment> tlEnvs = ThreadLocal.withInitial(() -> null);

  private EnvironmentsContext() {
  }

  /**
   * Returns environment for current thread.
   *
   * @return environment that assigned for current thread
   */
  public static Environment getCurrent() {
    return tlEnvs.get();
  }

  /**
   * Set environment for current thread.
   *
   * @param env environment that will be assigned for current thread
   */
  public static void setCurrent(Environment env) {
    log.debug("Set current environment: {}", env);
    tlEnvs.set(env);
  }

}
