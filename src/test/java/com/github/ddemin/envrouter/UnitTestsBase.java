package com.github.ddemin.envrouter;

import java.lang.reflect.Field;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.testng.annotations.BeforeMethod;

public class UnitTestsBase {

  public UnitTestsBase() {
    resetConfig();
  }

  @BeforeMethod(alwaysRun = true)
  public void resetConfig() {
    try {
      changeRouterConfigConstants("LOCK_TIMEOUT_MS", 60000);
      changeRouterConfigConstants("ENV_THREADS_MAX", 1);
      changeRouterConfigConstants("ENVS_DIRECTORY", "environments");
      changeRouterConfigConstants("ENV_DEFAULT", null);
      changeRouterConfigConstants("ENV_FORCED", null);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  protected void changeRouterConfigConstants(String constant, Object value) throws IllegalAccessException {
    Field field = FieldUtils.getField(RouterConfig.class, constant);
    FieldUtils.removeFinalModifier(field);
    FieldUtils.writeStaticField(field, value);
  }

}
