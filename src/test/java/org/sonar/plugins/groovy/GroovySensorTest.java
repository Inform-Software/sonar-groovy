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
package org.sonar.plugins.groovy;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroovySensorTest {

  private Settings settings = new Settings();
  private FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
  private ModuleFileSystem moduleFileSystem = mock(ModuleFileSystem.class);
  private DefaultFileSystem fileSystem = new DefaultFileSystem();
  private GroovySensor sensor = new GroovySensor(settings, fileLinesContextFactory, moduleFileSystem, fileSystem);

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    fileSystem.add(new DefaultInputFile("fake.groovy").setLanguage(Groovy.KEY));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_if_no_groovy_files() {
    Project project = mock(Project.class);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void compute_metrics() {
    testMetrics(false, 5.0);
  }

  @Test
  public void compute_metrics_ignoring_header_comment() {
    testMetrics(true, 1.0);
  }

  public void testMetrics(boolean headerComment, double expectedCommentMetric) {
    settings.appendProperty(GroovyPlugin.IGNORE_HEADER_COMMENTS, "" + headerComment);
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    java.io.File sourceDir = new java.io.File("src/test/resources/org/sonar/plugins/groovy/gmetrics");
    java.io.File sourceFile = new java.io.File(sourceDir, "Greeting.groovy");
    List<java.io.File> sourceDirs = Arrays.asList(sourceDir);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(Mockito.any(Resource.class))).thenReturn(fileLinesContext);
    when(moduleFileSystem.sourceDirs()).thenReturn(sourceDirs);
    when(moduleFileSystem.files(any(FileQuery.class))).thenReturn(Arrays.asList(sourceFile));

    sensor.analyse(project, context);

    File sonarFile = File.fromIOFile(new java.io.File(sourceDir, "Greeting.groovy"), sourceDirs);
    verify(context).saveMeasure(sonarFile, CoreMetrics.FILES, 1.0);
    verify(context).saveMeasure(sonarFile, CoreMetrics.CLASSES, 2.0);
    verify(context).saveMeasure(sonarFile, CoreMetrics.FUNCTIONS, 2.0);

    verify(context).saveMeasure(sonarFile, CoreMetrics.LINES, 27.0);
    verify(context).saveMeasure(sonarFile, CoreMetrics.NCLOC, 17.0);
    verify(context).saveMeasure(sonarFile, CoreMetrics.COMMENT_LINES, expectedCommentMetric);

    verify(context).saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, 4.0);
    verify(context).saveMeasure(
      Mockito.eq(sonarFile),
      Mockito.argThat(new IsMeasure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=0;2=2;4=0;6=0;8=0;10=0;12=0")));
    verify(context).saveMeasure(
      Mockito.eq(sonarFile),
      Mockito.argThat(new IsMeasure(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, "0=1;5=0;10=0;20=0;30=0;60=0;90=0")));
    // 5 times for comment because we register comment even when ignoring header comment
    verify(fileLinesContext, Mockito.times(5)).setIntValue(Mockito.eq(CoreMetrics.COMMENT_LINES_DATA_KEY), Mockito.anyInt(), Mockito.eq(1));
    verify(fileLinesContext, Mockito.times(17)).setIntValue(Mockito.eq(CoreMetrics.NCLOC_DATA_KEY), Mockito.anyInt(), Mockito.eq(1));
    verify(fileLinesContext).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 18, 1);
    verify(fileLinesContext).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 18, 1);
    verify(fileLinesContext).save();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("GroovySensor");
  }

}
