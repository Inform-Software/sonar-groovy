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
package org.sonar.plugins.groovy.surefire;

import org.junit.Test;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.resources.Project;
import org.sonar.plugins.groovy.foundation.Groovy;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SurefireSensorTest {

  private SurefireSensor sensor = new SurefireSensor();

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Groovy.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
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
  public void should_depend_on_coverage_sensors() {
    assertThat(sensor.dependsUponCoverageSensors()).isEqualTo(CoverageExtension.class);
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("Groovy SurefireSensor");
  }

}
