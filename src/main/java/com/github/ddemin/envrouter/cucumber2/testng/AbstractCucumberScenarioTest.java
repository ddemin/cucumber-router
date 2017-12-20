package com.github.ddemin.envrouter.cucumber2.testng;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.cucumber2.ScenarioWrapper;
import com.github.ddemin.envrouter.cucumber2.ScenariosUtils;
import cucumber.api.testng.CucumberFeatureWrapper;
import cucumber.api.testng.PickleEventWrapper;
import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCucumberScenarioTest extends AbstractCucumberTest<ScenarioWrapper> {

  protected abstract void processFailedLocking(EnvironmentLock<ScenarioWrapper> lock);

  @Override
  List<ScenarioWrapper> wrapEntities() {
    Map<CucumberFeature, List<PickleEvent>> scenariosMap = Arrays.stream(tlCukeRunner.get().provideScenarios())
        .map(objs ->
            new Pair<>(
                ((CucumberFeatureWrapper) objs[1]).getCucumberFeature(),
                ((PickleEventWrapper) objs[0]).getPickleEvent()
            )
        )
        .collect(
            Collectors.groupingBy(
                Pair::getKey,
                mapping(Pair::getValue, toList())
            )
        );
    return ScenariosUtils.wrapScenarios(scenariosMap);
  }

  @Override
  void runCucumberEntity(ScenarioWrapper cucumberEntityWrapper) {
    try {
      tlCukeRunner.get().runScenario(cucumberEntityWrapper.getEntity());
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

}
