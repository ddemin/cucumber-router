package com.github.ddemin.envrouter.cucumber2;

import static com.github.ddemin.envrouter.cucumber2.FeaturesQueues.ANY_ENVIRONMENT;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.base.Environment;
import cucumber.runtime.model.CucumberFeature;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Slf4j
public class FeaturesUtils {

  private static final String CUKE_ENV_TAG = "@Env";
  private static final String CUKE_PRIORITY_TAG = "@Priority";

  private FeaturesUtils() {
  }

  /**
   * Wrap cucumber-jvm feature and parse its priority tag and required environment tag.
   *
   * @param feature cucumber-jvm feature
   * @return list of wrapped features
   */
  public static FeatureWrapper wrapFeature(CucumberFeature feature) {
    log.debug("Wrap feature: {}", feature.getUri());
    return new FeatureWrapper(
        feature,
        getFeatureRequiredEnvironment(feature),
        getFeaturePriority(feature)
    );
  }

  /**
   * Wrap cucumber-jvm features and parse its priority tag and required environment tag.
   *
   * @param features cucumber-jms features
   * @return list of wrapped features
   */
  public static List<FeatureWrapper> wrapFeatures(List<CucumberFeature> features) {
    log.info(
        "Wrap features: {}",
        features.stream().map(CucumberFeature::getUri).collect(Collectors.toList())
    );
    return features.stream()
        .map(FeaturesUtils::wrapFeature)
        .collect(Collectors.toList());
  }

  /**
   * Get list of queues that have required environments that don't exist.
   *
   * @param queuesGroup queues of wrapped features that grouped by environments name
   * @param definedEnvs set of environments that exist now
   * @return list of queues for environments that don't exist (weren't provided)
   */
  public static List<Entry<String, Queue<FeatureWrapper>>> getQueuesForUndefinedEnvs(
      FeaturesQueues queuesGroup,
      Set<Environment> definedEnvs
  ) {
    log.debug("Get all queues for undefined environments...");
    return queuesGroup.getQueuesMap().entrySet().stream()
        .filter(
            entry ->
                definedEnvs.stream()
                    .noneMatch(
                        confEnv ->
                            confEnv.getName().startsWith(entry.getKey())
                    )
        )
        .filter(entry -> !entry.getKey().equals(ANY_ENVIRONMENT))
        .filter(entry -> entry.getValue().size() > 0)
        .collect(Collectors.toList());
  }

  private static int getFeaturePriority(CucumberFeature feature) {
    return extractTagEnding(feature, CUKE_PRIORITY_TAG)
        .filter(priorityStr -> priorityStr.matches("[\\d]+"))
        .map(Integer::parseInt)
        .orElse(Integer.MAX_VALUE);
  }

  private static String getFeatureRequiredEnvironment(CucumberFeature feature) {
    return extractTagEnding(feature, CUKE_ENV_TAG)
        .orElse(RouterConfig.ENV_DEFAULT == null ? ANY_ENVIRONMENT : RouterConfig.ENV_DEFAULT)
        .toLowerCase();
  }

  private static Optional<String> extractTagEnding(CucumberFeature feature, String str) {
    return feature.getGherkinFeature().getFeature().getTags().stream()
        .filter(tag -> tag.getName().startsWith(str))
        .map(tag -> StringUtils.substringAfterLast(tag.getName(), str))
        .findFirst();
  }

}
