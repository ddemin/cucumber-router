package com.github.ddemin.envrouter;

import com.github.ddemin.envrouter.cucumber2.FeatureWrapper;
import cucumber.runtime.model.CucumberFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FeatureWrapperTests {

  @Mock
  CucumberFeature feature;

  @BeforeClass
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForNullInConstructor1() {
    new FeatureWrapper(null, "test", 1);
  }

  @SuppressFBWarnings
  @Test(expectedExceptions = NullPointerException.class)
  public void catchNpeForNullInConstructor2() {
    new FeatureWrapper(feature, null, 1);
  }

}
