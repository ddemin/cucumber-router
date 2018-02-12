package com.github.ddemin.envrouter.util;

import static io.qameta.allure.util.AspectUtils.getParameters;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Created by Dmitrii Demin on 21.09.2017.
 */
@Aspect
@Slf4j
public class HamcrestAllureAspects {

  private static final AllureLifecycle ALLURE = Allure.getLifecycle();

  private static Object wrapToAllureStep(String title, ProceedingJoinPoint point) {
    log.trace("Wrap hamcrest assertThat() call as Allure step '{}'", title);

    final String uuid = UUID.randomUUID().toString();
    final StepResult result = new StepResult()
        .withName(title)
        .withParameters(
            getParameters((MethodSignature) point.getSignature(), point.getArgs())[2]
        );

    ALLURE.startStep(uuid, result);
    try {
      final Object proceed = point.proceed();
      ALLURE.updateStep(uuid, s -> s.withStatus(Status.PASSED));
      return proceed;
    } catch (AssertionError error) {
      ALLURE.updateStep(uuid, s -> s
          .withParameters(
              getParameters((MethodSignature) point.getSignature(), point.getArgs())[1]
          )
          .withStatus(getStatus(error).orElse(Status.FAILED))
          .withStatusDetails(getStatusDetails(error).orElse(null)));
      throw error;
    } catch (Throwable ex) {
      ALLURE.updateStep(uuid, s -> s
          .withParameters(
              getParameters((MethodSignature) point.getSignature(), point.getArgs())[1]
          )
          .withStatus(getStatus(ex).orElse(Status.BROKEN))
          .withStatusDetails(getStatusDetails(ex).orElse(null)));
      throw new RuntimeException(ex);
    } finally {
      ALLURE.stopStep(uuid);
    }
  }

  /**
   * Wrap execution of Hamcrest #assertThat method.
   *
   * @param point MatcherAssert#assertThat
   * @return result of wrapped method execution
   */
  @Around("execution(* org.hamcrest.MatcherAssert.assertThat(..)) && args(String,Object,Object)")
  public Object hamcrestAssertThat(final ProceedingJoinPoint point) {
    return wrapToAllureStep(
        String.format(
            "%s",
            point.getArgs()[0].toString().isEmpty()
                ? "Assertion must be success"
                : point.getArgs()[0]
        ),
        point
    );
  }

  /**
   * Wrap execution of Hamcrest #assertThat method.
   *
   * @param point MatcherAssert#assertThat method
   * @return result of wrapped method execution
   */
  @Around("execution(* org.hamcrest.MatcherAssert.assertThat(..)) && args(String,boolean)")
  public Object hamcrestAssertThatSimple(final ProceedingJoinPoint point) {
    return wrapToAllureStep(
        String.format(
            "%s",
            point.getArgs()[0].toString().isEmpty()
                ? "Assertion must be success"
                : point.getArgs()[0]
        ),
        point
    );
  }

}
