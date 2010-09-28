package org.sonar.plugins.groovy.cobertura;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.test.TestUtils;

import static org.mockito.Mockito.mock;

public class CoberturaSensorTest {

  private CoberturaSensor sensor;

  @Before
  public void setUp() throws Exception {
    sensor = new CoberturaSensor(null, null);
  }

  @Test
  public void shouldParseReport() {
    // see SONARPLUGINS-696
    SensorContext context = mock(SensorContext.class);
    sensor.parseReport(TestUtils.getResource(getClass(), "coverage.xml"), context);
  }
}
