/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.groovy.cobertura;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.test.TestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaSensorTest {

  private CoberturaMavenPluginHandler mavenPluginHandler;
  private CoberturaSensor sensor;

  @Before
  public void setUp() throws Exception {
    mavenPluginHandler = mock(CoberturaMavenPluginHandler.class);
    sensor = new CoberturaSensor(mavenPluginHandler);
  }

  /**
   * See SONARPLUGINS-696
   */
  @Test
  public void should_parse_report() {
    SensorContext context = mock(SensorContext.class);
    sensor.parseReport(TestUtils.getResource(getClass(), "coverage.xml"), context);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Groovy.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    assertThat(sensor.getMavenPluginHandler(project)).isNull();
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    assertThat(sensor.getMavenPluginHandler(project)).isSameAs(mavenPluginHandler);
  }

  @Test
  public void should_not_execute_if_static_analysis() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Groovy.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_on_java_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("java");
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("Groovy CoberturaSensor");
  }

}
