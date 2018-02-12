package com.github.ddemin.envrouter.util;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Dmitrii Demin on 22.09.2017.
 */
@Slf4j
public class AllureUtils {

  private AllureUtils() {
  }

  public static void saveAsStep(String name, Runnable action) {
    step(name, action);
  }

  public static <T> T saveAsStep(String name, Supplier<T> action) {
    return step(name, action);
  }

  /**
   * Save test as BROKEN in Allure report with name and result message.
   *
   * @param testName test name that will be displayed in Allure report
   * @param message test result message
   */
  public static void saveAsBrokenTest(String testName, String message) {
    log.trace("Save '{}' with message '{}'", testName, message);

    String containerUuid
        = UUID.nameUUIDFromBytes((testName + "container")
        .getBytes(Charset.defaultCharset()))
        .toString();
    final TestResultContainer containerRes = new TestResultContainer()
        .withUuid(containerUuid)
        .withName(testName)
        .withStart(System.currentTimeMillis());
    Allure.getLifecycle().startTestContainer(containerRes);

    String testUuid
        = UUID.nameUUIDFromBytes(testName.getBytes(Charset.defaultCharset())).toString();
    TestResult testRes = new TestResult()
        .withUuid(testUuid)
        .withName(testName)
        .withFullName(testName)
        .withHistoryId(testUuid);
    Allure.getLifecycle().scheduleTestCase(containerUuid, testRes);
    Allure.getLifecycle().startTestCase(testUuid);
    Allure.getLifecycle().updateTestCase(
        testUuid,
        testResult -> testResult
            .withStatus(Status.BROKEN)
            .withStatusDetails(
                new StatusDetails().withMessage(message)
            )
    );

    try {
      Thread.sleep(50);
    } catch (InterruptedException ignored) {
      log.warn(ignored.getMessage());
    }
    Allure.getLifecycle().stopTestCase(testUuid);
    Allure.getLifecycle().writeTestCase(testUuid);

    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
      log.warn(ignored.getMessage());
    }
    Allure.getLifecycle().stopTestContainer(containerUuid);
    Allure.getLifecycle().writeTestContainer(containerUuid);
  }

  @Step("{name}")
  private static void step(String name, Runnable action) {
    action.run();
  }

  @Step("{name}")
  private static <T> T step(String name, Supplier<T> action) {
    return action.get();
  }

}
