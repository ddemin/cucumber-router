package com.github.ddemin.envrouter.cucumber2;

import static java.lang.String.format;

import com.github.ddemin.envrouter.base.TestEntityWrapper;
import cucumber.runtime.model.CucumberFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Getter
@Slf4j(topic = "wrapper")
public class FeatureWrapper extends TestEntityWrapper<CucumberFeature> {

  @SuppressFBWarnings
  public FeatureWrapper(@NonNull CucumberFeature entity, String requiredEnvironmentName, int priority) {
    super(entity, requiredEnvironmentName, priority);
  }

  @Override
  public String toString() {
    return format(
        "%s (Env: %s, Priority: %d)",
        getEntity().getUri(),
        getRequiredEnvironmentName(),
        getPriority()
    );
  }

  @Override
  public String getName() {
    return getEntity().getGherkinFeature().getFeature().getName();
  }

  @Override
  public String getPath() {
    return getEntity().getUri();
  }

}
