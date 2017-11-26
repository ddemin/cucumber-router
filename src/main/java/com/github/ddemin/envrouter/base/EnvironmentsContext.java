package com.github.ddemin.envrouter.base;

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
    tlEnvs.set(env);
  }

}
