package com.github.ddemin.envrouter;

public class RouterConfig {

  public static final String ENVS_DIRECTORY = System.getProperty("router.envs", "environments");
  public static final String ENV_DEFAULT = System.getProperty("router.envs.default");
  public static final int ENV_THREADS_MAX
      = Integer.parseInt(System.getProperty("router.threadsPerEnv", "1"));
  public static final int LOCK_TIMEOUT_MS
      = Integer.parseInt(System.getProperty("router.lock.timeout", "60000"));

}
