package com.github.ddemin.envrouter.cucumber2;

import static java.lang.String.format;

import cucumber.runtime.model.CucumberFeature;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Getter
@Slf4j(topic = "wrapper")
@AllArgsConstructor
public class FeatureWrapper {

  private CucumberFeature feature;
  private String requiredEnvironmentName;
  private int priority;

  @Override
  public String toString() {
    return format(
        "%s (Env: %s, Priority: %d)",
        feature.getUri(),
        requiredEnvironmentName,
        priority
    );
  }

}
