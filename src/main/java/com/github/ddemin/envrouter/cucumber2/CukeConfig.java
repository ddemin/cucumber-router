package com.github.ddemin.envrouter.cucumber2;

import static com.github.ddemin.envrouter.cucumber2.CukeConfig.CukeTestNgConfigKeys.CONVERTERS_KEY;
import static com.github.ddemin.envrouter.cucumber2.CukeConfig.CukeTestNgConfigKeys.GUICE_MODULES_KEY;
import static com.github.ddemin.envrouter.cucumber2.CukeConfig.CukeTestNgConfigKeys.TAGS_KEY;

import com.google.common.base.Splitter;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CukeConfig {

  /**
   * Cucumber-jvm tag groups that delimited by ';' char and this equivalent to '--tags @someA --tags @someB'.
   * Inside group tags can be delimited by " or " or ',' and this equivalent to '--tags @someA or @someB'
   * or '--tags @someA,@someB'.
   * Each tag must havel name starts with '@'. See https://github.com/cucumber/cucumber/wiki/Tags
   */
  public static final String TAGS = System.getProperty(TAGS_KEY);
  public static final List<String> CONVERTERS = Splitter.on(",").omitEmptyStrings().splitToList(
      System.getProperty(CONVERTERS_KEY, "")
  );
  public static final List<String> GUICE_MODULES = Splitter.on(",").omitEmptyStrings().splitToList(
      System.getProperty(GUICE_MODULES_KEY, "")
  );

  public static class CukeTestNgConfigKeys {

    public static final String TAGS_KEY = "cuke.tags";
    public static final String CONVERTERS_KEY = "cuke.converters";
    public static final String GUICE_MODULES_KEY = "cuke.guice.modules";

    private CukeTestNgConfigKeys() {
    }

  }

}