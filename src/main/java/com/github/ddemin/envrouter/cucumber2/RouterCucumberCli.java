package com.github.ddemin.envrouter.cucumber2;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import com.github.ddemin.envrouter.RouterConfig;
import com.github.ddemin.envrouter.base.Environment;
import com.github.ddemin.envrouter.base.EnvironmentLock;
import com.github.ddemin.envrouter.base.EnvironmentLocksController;
import com.github.ddemin.envrouter.base.EnvironmentsContext;
import com.github.ddemin.envrouter.cucumber2.testng.AbstractCucumberTest;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RouterCucumberCli extends AbstractCucumberTest {

  public static void main(String[] argv) throws Throwable {
    byte exitstatus = run(argv, Thread.currentThread().getContextClassLoader());
    System.exit(exitstatus);
  }

  /**
   * Launches the Cucumber-JVM command line.
   *
   * @param argv runtime options. See details in the {@code cucumber.api.cli.Usage.txt} resource.
   * @param classLoader classloader used to load the runtime
   * @return 0 if execution was successful, 1 if it was not (test failures)
   * @throws IOException if resources couldn't be loaded during the run.
   */
  public static byte run(String[] argv, ClassLoader classLoader) throws IOException {
    RuntimeOptions runtimeOptions = new RuntimeOptions(new ArrayList<>(asList(argv)));
    ResourceLoader resourceLoader = new MultiLoader(classLoader);
    ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
    Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions) {
      @Override
      public void runFeature(CucumberFeature feature) {
        FeatureWrapper wrapper = FeaturesUtils.wrapFeature(feature);

        Path pathToProperties;
        try {
          pathToProperties = Paths.get(
              AbstractCucumberTest.class
                  .getClassLoader()
                  .getResource(RouterConfig.ENVS_DIRECTORY)
                  .toURI()
          )
              .resolve(wrapper.getRequiredEnvironmentName());
        } catch (URISyntaxException ex) {
          throw new RuntimeException(ex);
        }

        EnvironmentLocksController envsController;
        envsController = new EnvironmentLocksController(
            new Environment(pathToProperties),
            1
        );

        Environment envForTest = envsController.getByName(wrapper.getRequiredEnvironmentName());
        if (envForTest == null) {
          throw new RuntimeException(
              format("Environment %s for feature %s isn't defined",
                  wrapper.getRequiredEnvironmentName(),
                  wrapper.getEntity().getUri())
          );
        }
        EnvironmentsContext.setCurrent(envForTest);

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
      case FAILURE_UNDEFINED_ENV:
        throw new RuntimeException(
            format(
                "Feature %s has undefined environment %s",
                wrapper.getEntity().getUri(),
                wrapper.getRequiredEnvironmentName()
            )
        );
      case FAILURE_NO_TARGET_ENTITIES:
        throw new IllegalStateException(
            "No any waiting feature was found for all available environments"
        );
      default:
        throw new IllegalStateException(
            "Unexpected lock status: " + lock.getLockStatus()
        );
    }
  }

}
