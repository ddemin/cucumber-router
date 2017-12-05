package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys.ENVS_DIRECTORY_KEY;
import static com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys.ENV_DEFAULT_KEY;
import static com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys.ENV_FORCED_KEY;
import static com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys.ENV_THREADS_MAX_KEY;
import static com.github.ddemin.envrouter.RouterConfig.RouterConfigKeys.LOCK_TIMEOUT_MS_KEY;

public class RouterConfig {

  private RouterConfig() {}

  public static final String ENVS_DIRECTORY = System.getProperty(ENVS_DIRECTORY_KEY, "environments");
  public static final String ENV_DEFAULT = System.getProperty(ENV_DEFAULT_KEY);
  public static final String ENV_FORCED = System.getProperty(ENV_FORCED_KEY);
  public static final int ENV_THREADS_MAX
      = Integer.parseInt(System.getProperty(ENV_THREADS_MAX_KEY, "1"));
  public static final int LOCK_TIMEOUT_MS
      = Integer.parseInt(System.getProperty(LOCK_TIMEOUT_MS_KEY, "60000"));

  public static class RouterConfigKeys {

    private RouterConfigKeys() {}

    public static final String ENVS_DIRECTORY_KEY = "router.envs.dir";
    public static final String ENV_DEFAULT_KEY = "router.envs.default";
    public static final String ENV_FORCED_KEY = "router.envs.forced";
    public static final String ENV_THREADS_MAX_KEY = "router.threadsPerEnv";
    public static final String LOCK_TIMEOUT_MS_KEY = "router.lock.timeout";
  }

}
