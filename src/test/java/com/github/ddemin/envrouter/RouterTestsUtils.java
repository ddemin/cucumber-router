package com.github.ddemin.envrouter;

import java.lang.reflect.Field;
import org.apache.commons.lang3.reflect.FieldUtils;

public class RouterTestsUtils {

  private RouterTestsUtils() {}

  public static void changeRouterConfigConstants(String constant, Object value) throws IllegalAccessException {
    Field field = FieldUtils.getField(RouterConfig.class, constant);
    FieldUtils.removeFinalModifier(field);
    FieldUtils.writeStaticField(field, value);
  }

}
