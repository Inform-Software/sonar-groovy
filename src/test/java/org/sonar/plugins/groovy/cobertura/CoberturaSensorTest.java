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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaSensorTest {

  private Settings settings ;
  private CoberturaSensor sensor;
  private ModuleFileSystem moduleFileSystem;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    settings.addProperties(ImmutableMap.of(GroovyPlugin.COBERTURA_REPORT_PATH, "src/test/resources/org/sonar/plugins/groovy/cobertura/coverage.xml"));
    moduleFileSystem = mock(ModuleFileSystem.class);
    sensor = new CoberturaSensor(settings, moduleFileSystem);
  }

  /**
   * See SONARPLUGINS-696
   */
  @Test
  public void should_parse_report() {
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(context.getResource(any(File.class))).thenReturn(mock(File.class));
    sensor.analyse(project, context);
    verify(context, times(298)).saveMeasure(any(File.class), any(Measure.class));
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    doReturn(Collections.singletonList(new File("fake.groovy"))).when(moduleFileSystem).files(any(FileQuery.class));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_if_static_analysis() {
    Project project = mock(Project.class);
    doReturn(Collections.singletonList(new File("fake.groovy"))).when(moduleFileSystem).files(any(FileQuery.class));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_if_no_groovy_files() {
    Project project = mock(Project.class);
    doReturn(Collections.emptyList()).when(moduleFileSystem).files(any(FileQuery.class));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("Groovy CoberturaSensor");
  }

}
