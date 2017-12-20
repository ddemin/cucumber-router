package com.github.ddemin.envrouter.cucumber2.testng;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import com.github.ddemin.envrouter.cucumber2.FeaturesUtils;
import cucumber.api.testng.CucumberFeatureWrapper;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCucumberFeatureTest extends AbstractCucumberTest<FeatureWrapper> {

  protected abstract void processFailedLocking(EnvironmentLock<FeatureWrapper> lock);

  @Override
  List<FeatureWrapper> wrapEntities() {
    return FeaturesUtils.wrapFeatures(
        Arrays.stream(tlCukeRunner.get().provideFeatures())
            .flatMap(
                objs -> Arrays.stream(objs)
                    .map(obj -> ((CucumberFeatureWrapper) obj).getCucumberFeature())
            )
            .distinct()
            .collect(Collectors.toList())
    );
  }

  @Override
  void runCucumberEntity(FeatureWrapper cucumberEntityWrapper) {
    tlCukeRunner.get().runCucumber(cucumberEntityWrapper.getEntity());
  }

}
