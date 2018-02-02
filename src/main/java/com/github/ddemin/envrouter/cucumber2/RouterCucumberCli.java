package com.github.ddemin.envrouter.cucumber2;

import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.SUCCESS_HARD_LOCKED;
import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.SUCCESS_LOCKED;
import static com.github.ddemin.envrouter.cucumber2.CukeConfig.CONVERTERS;
import static com.github.ddemin.envrouter.cucumber2.CukeConfig.GUICE_MODULES;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentsUtils;
import com.github.ddemin.envrouter.base.EnvsLocksController;
import com.github.ddemin.envrouter.base.TestEntitiesQueues;
import com.github.ddemin.envrouter.cucumber2.testng.AbstractCucumberFeatureTest;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.inject.Guice;
import com.google.inject.Module;
import cucumber.deps.com.thoughtworks.xstream.annotations.XStreamConverter;
import cucumber.deps.com.thoughtworks.xstream.converters.ConverterMatcher;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.annotation.AnnotationParser;

@Slf4j
public class RouterCucumberCli extends AbstractCucumberFeatureTest {

  public static final RouterCucumberCli INSTANCE = new RouterCucumberCli();

  public static void main(String[] argv) throws Throwable {
    byte exitStatus = run(argv, Thread.currentThread().getContextClassLoader());
    System.exit(exitStatus);
  }

  /**
   * Launches the Cucumber-JVM command line.
   *
   * @param argv runtime options. See details in the {@code cucumber.api.cli.Usage.txt} resource.
   * @param classLoader classloader used to load the runtime
   * @return 0 if execution was successful, 1 if it was not (demo failures)
   * @throws IOException if resources couldn't be loaded during the run.
   */
  public static byte run(String[] argv, ClassLoader classLoader) throws IOException {
    injectGuiceModules(GUICE_MODULES);

    RuntimeOptions runtimeOptions = new RuntimeOptions(new ArrayList<>(asList(argv)));
    registerConverters(runtimeOptions, CONVERTERS);

    ResourceLoader resourceLoader = new MultiLoader(classLoader);
    ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);

    Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions) {
      @Override
      public void runFeature(CucumberFeature feature) {
        TestEntitiesQueues<FeatureWrapper> queues = new TestEntitiesQueues<>();
        queues.add(FeaturesUtils.wrapFeature(feature));

        EnvironmentLock<FeatureWrapper> lock = EnvsLocksController.findUntestedEntityAndLockEnv(queues);
        if (lock.getLockStatus() != SUCCESS_LOCKED && lock.getLockStatus() != SUCCESS_HARD_LOCKED) {
          INSTANCE.processFailedLocking(lock);
        }

        EnvironmentsUtils.setCurrent(lock.getEnvironment());

        List<PickleEvent> pickleEvents = compileFeature(feature);
        for (PickleEvent pickleEvent : pickleEvents) {
          if (matchesFilters(pickleEvent)) {
            getRunner().runPickle(pickleEvent);
          }
        }
      }
    };

    runtime.run();

    return runtime.exitStatus();
  }

  @Override
  protected void processFailedLocking(EnvironmentLock<FeatureWrapper> lock) {
    FeatureWrapper wrapper = lock.getTargetEntity();
    switch (lock.getLockStatus()) {
      case FAILURE_NO_ENTITY_FOR_AVAILABLE_ENVS:
      case FAILURE_NO_AVAILABLE:
        throw new RuntimeException("Required environment is busy");
      case FAILURE_UNDEFINED_ENV:
        throw new RuntimeException(
            format(
                "Feature %s has undefined environment %s",
                wrapper.getEntity().getUri(),
                wrapper.getRequiredEnvironmentName()
            )
        );
      case FAILURE_NO_TARGET_ENTITIES:
        throw new IllegalStateException("No any untested features were found for available environments");
      default:
        throw new IllegalStateException(
            "Unexpected lock status: " + lock.getLockStatus()
        );
    }
  }

  private static void injectGuiceModules(List<String> packagesNames) {
    List<Module> guiceModules = packagesNames.stream()
        .flatMap(RouterCucumberCli::getStreamOfClassesFrom)
        .filter(Module.class::isAssignableFrom)
        .map(
            clz -> {
              try {
                return (Module) clz.newInstance();
              } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
              }
            }
        )
        .collect(Collectors.toList());
    Guice.createInjector(guiceModules);
  }

  // TODO Check converters overlapping
  private static void registerConverters(RuntimeOptions runtimeOptions, List<String> packagesNames) {
    List<Annotation> convertersAnnotations = packagesNames.stream()
        .flatMap(RouterCucumberCli::getStreamOfClassesFrom)
        .filter(ConverterMatcher.class::isAssignableFrom)
        .map(
            converterClass -> {
              Map<String, Object> annPropertiesMap = new HashMap<>();
              annPropertiesMap.put("value", converterClass);
              annPropertiesMap.put("priority", 0);
              return AnnotationParser.annotationForMap(XStreamConverter.class, annPropertiesMap);
            }
        )
        .collect(Collectors.toList());

    if (!convertersAnnotations.isEmpty()) {
      try {
        Field field = runtimeOptions.getClass().getDeclaredField("converters");

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.setAccessible(true);
        field.set(runtimeOptions, convertersAnnotations);
        field.setAccessible(false);

        modifiersField.setInt(field, field.getModifiers() & Modifier.FINAL);
        modifiersField.setAccessible(false);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private static Stream<Class<?>> getStreamOfClassesFrom(String packageOrClzFullName) {
    try {
      if (Thread.currentThread().getContextClassLoader()
          .getResource(packageOrClzFullName.replace('.', '/')) != null) {
        return ClassPath.from(RouterCucumberCli.class.getClassLoader())
            .getTopLevelClassesRecursive(packageOrClzFullName)
            .stream()
            .map(ClassInfo::load);
      } else {
        return Stream.of(RouterCucumberCli.class.getClassLoader().loadClass(packageOrClzFullName));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
