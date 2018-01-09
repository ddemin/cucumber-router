package com.github.ddemin.envrouter.cucumber2;

import static com.github.ddemin.envrouter.cucumber2.CukeTags.CUKE_ENV_TAG;
import static com.github.ddemin.envrouter.cucumber2.CukeTags.CUKE_HARDLOCK_TAG;
import static com.github.ddemin.envrouter.cucumber2.CukeTags.CUKE_PRIORITY_TAG;

import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Dmitrii Demin on 18.09.2017.
 */
@Slf4j
@UtilityClass
// TODO Deduplicate
public class FeaturesUtils {

  /**
   * Wrap cucumber-jvm feature and parse its priority tag and required environment tag.
   *
   * @param feature cucumber-jvm feature
   * @return list of wrapped features
   */
  public static FeatureWrapper wrapFeature(@NonNull CucumberFeature feature) {
    log.debug("Wrap feature: {}", feature.getUri());
    return new FeatureWrapper(
        feature,
        getFeatureRequiredEnvironment(feature),
        getFeaturePriority(feature),
        getHardLockingRequirement(feature)
    );
  }

  /**
   * Wrap cucumber-jvm features and parse its priority tag and required environment tag.
   *
   * @param features cucumber-jms features
   * @return list of wrapped features
   */
  public static List<FeatureWrapper> wrapFeatures(@NonNull List<CucumberFeature> features) {
    log.info(
        "Wrap features: {}",
        features.stream().map(CucumberFeature::getUri).collect(Collectors.toList())
    );
    return features.stream()
        .map(FeaturesUtils::wrapFeature)
        .collect(Collectors.toList());
  }

  static int getFeaturePriority(@NonNull CucumberFeature feature) {
    return extractTagEnding(feature, CUKE_PRIORITY_TAG)
        .filter(priorityStr -> priorityStr.matches("[\\d]+"))
        .map(Integer::parseInt)
        .orElse(Integer.MAX_VALUE);
  }

  static String getFeatureRequiredEnvironment(@NonNull CucumberFeature feature) {
    return extractTagEnding(feature, CUKE_ENV_TAG).orElse(null);
  }

  static boolean getHardLockingRequirement(@NonNull CucumberFeature feature) {
    return extractTagEnding(feature, CUKE_HARDLOCK_TAG).isPresent();
  }

  private static Optional<String> extractTagEnding(@NonNull CucumberFeature feature,
      @NonNull String str) {
    return feature.getGherkinFeature().getFeature().getTags().stream()
        .filter(tag -> tag.getName().startsWith(str))
        .map(tag -> StringUtils.substringAfterLast(tag.getName(), str))
        .findFirst();
  }

}
