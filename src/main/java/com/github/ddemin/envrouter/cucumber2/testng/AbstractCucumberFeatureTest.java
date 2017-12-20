package com.github.ddemin.envrouter.cucumber2.testng;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import com.github.ddemin.envrouter.cucumber2.FeaturesUtils;
import cucumber.api.testng.CucumberFeatureWrapper;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCucumberFeatureTest extends AbstractCucumberTest<FeatureWrapper> {

  protected abstract void processFailedLocking(EnvironmentLock<FeatureWrapper> lock);

  @Override
  int initQueues() {
    TestEntitiesQueues<FeatureWrapper> envQueues = getEnvsQueuesForThisClass();
    envQueues.addAll(
        FeaturesUtils.wrapFeatures(
            Arrays.stream(cukeRunner.get().provideFeatures())
                .flatMap(
                    objs -> Arrays.stream(objs)
                        .map(obj -> ((CucumberFeatureWrapper) obj).getCucumberFeature())
                )
                .distinct()
                .collect(Collectors.toList())
        )
    );
    log.info("Save all features to queues. Processed {}", envQueues.entitiesInAllQueues());
    return envQueues.entitiesInAllQueues();
  }

  @Override
  void runCucumberEntity(FeatureWrapper cucumberEntityWrapper) {
    cukeRunner.get().runCucumber(cucumberEntityWrapper.getEntity());
  }

}
