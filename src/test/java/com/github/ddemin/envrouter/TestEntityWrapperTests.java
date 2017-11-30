package com.github.ddemin.envrouter;

import com.github.ddemin.envrouter.base.TestEntityWrapper;
import cucumber.runtime.model.CucumberFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TestEntityWrapperTests {

  @Mock
  CucumberFeature feature;

  @BeforeClass
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForNullInConstructor1() {
    new TestEntityWrapper<String>(null, "test", 1);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForNullInConstructor2() {
    new TestEntityWrapper<String>("entity", null, 1);
  }

}
