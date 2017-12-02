package com.github.ddemin.envrouter;

import static com.github.ddemin.envrouter.base.EnvironmentLock.LockStatus.SUCCESS_LOCKED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.ddemin.envrouter.base.EnvironmentLock;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EnvironmentLockTests {

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "lockStatus")
  public void catchNpe() {
    new EnvironmentLock<>(null);
  }

  @Test
  public void checkContsructor() {
    EnvironmentLock envLock = new EnvironmentLock<>(SUCCESS_LOCKED);
    assertThat(envLock.getLockStatus(), equalTo(SUCCESS_LOCKED));
  }

}
