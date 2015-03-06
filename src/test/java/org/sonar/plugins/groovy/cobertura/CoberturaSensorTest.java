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
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaSensorTest {

  private Settings settings;
  private CoberturaSensor sensor;
  private DefaultFileSystem fileSystem;
  private Project project;
  private SensorContext context;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    settings.addProperties(ImmutableMap.of(GroovyPlugin.COBERTURA_REPORT_PATH, "src/test/resources/org/sonar/plugins/groovy/cobertura/coverage.xml"));
    fileSystem = new DefaultFileSystem();
    sensor = new CoberturaSensor(settings, fileSystem);
    project = mock(Project.class);
    context = mock(SensorContext.class);
  }

  /**
   * See SONARPLUGINS-696
   */
  @Test
  public void should_parse_report() {
    FileSystem mockfileSystem = mock(FileSystem.class);
    when(mockfileSystem.predicates()).thenReturn(fileSystem.predicates());
    when(mockfileSystem.inputFile(any(FilePredicate.class))).thenReturn(mock(InputFile.class));
    sensor = new CoberturaSensor(settings, mockfileSystem);
    sensor.analyse(project, context);
    verify(context, times(298)).saveMeasure(any(InputFile.class), any(Measure.class));
  }

  @Test
  public void should_not_save_any_measure_if_report_not_found() {
    FileSystem mockfileSystem = mock(FileSystem.class);
    when(mockfileSystem.predicates()).thenReturn(fileSystem.predicates());
    when(mockfileSystem.inputFile(any(FilePredicate.class))).thenReturn(null);
    sensor = new CoberturaSensor(settings, mockfileSystem);
    sensor.analyse(project, context);
    verify(context, never()).saveMeasure(any(InputFile.class), any(Measure.class));
  }

  @Test
  public void should_not_parse_report_if_settings_does_not_contain_report_path() {
    sensor = new CoberturaSensor(new Settings(), new DefaultFileSystem());
    sensor.analyse(project, context);
    verify(context, never()).saveMeasure(any(InputFile.class), any(Measure.class));
  }

  @Test
  public void should_not_parse_report_if_report_does_not_exist() {
    Settings settings = new Settings();
    settings.addProperties(ImmutableMap.of(GroovyPlugin.COBERTURA_REPORT_PATH, "org/sonar/plugins/groovy/cobertura/fake-coverage.xml"));
    sensor = new CoberturaSensor(settings, new DefaultFileSystem());
    sensor.analyse(project, context);
    verify(context, never()).saveMeasure(any(InputFile.class), any(Measure.class));
  }

  @Test
  public void should_use_relative_path_to_get_report() {
    Settings settings = new Settings();
    settings.addProperties(ImmutableMap.of(GroovyPlugin.COBERTURA_REPORT_PATH, "//org/sonar/plugins/groovy/cobertura/fake-coverage.xml"));
    sensor = new CoberturaSensor(settings, new DefaultFileSystem());
    sensor.analyse(project, context);
    verify(context, never()).saveMeasure(any(InputFile.class), any(Measure.class));
  }

  @Test
  public void should_execute_on_project() {
    fileSystem.add(new DefaultInputFile("fake.groovy").setLanguage(Groovy.KEY));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_if_no_groovy_files() {
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("Groovy CoberturaSensor");
  }

}
