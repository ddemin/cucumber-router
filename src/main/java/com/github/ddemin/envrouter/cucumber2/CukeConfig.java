package com.github.ddemin.envrouter.cucumber2;

import static com.github.ddemin.envrouter.cucumber2.CukeConfig.CukeTestNgConfigKeys.TAGS_KEY;

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

  public static class CukeTestNgConfigKeys {

    public static final String TAGS_KEY = "cuke.tags";

    private CukeTestNgConfigKeys() {
    }

  }

}
