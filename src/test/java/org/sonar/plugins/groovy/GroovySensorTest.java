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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroovySensorTest {

  private GroovySensor sensor = new GroovySensor(new Groovy());

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Groovy.KEY);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_on_java_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("java");
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void compute_metrics(){
    testMetrics(false, 2.0);
  }
  @Test
  public void compute_metrics_ignoring_header_comment() {
    testMetrics(true, 1.0);
  }

  public void testMetrics(boolean headerComment, double expectedCommentMetric) {
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    java.io.File sourceDir = new java.io.File("src/test/resources/org/sonar/plugins/groovy/gmetrics");
    java.io.File sourceFile = new java.io.File(sourceDir, "Greeting.groovy");
    List<java.io.File> sourceDirs = Arrays.asList(sourceDir);
    when(pfs.getSourceDirs()).thenReturn(sourceDirs);
    when(pfs.getSourceCharset()).thenReturn(Charset.forName("UTF-8"));
    when(pfs.getSourceFiles(new Groovy())).thenReturn(Arrays.asList(sourceFile));
    when(project.getFileSystem()).thenReturn(pfs);
    when(project.getProperty(GroovyPlugin.IGNORE_HEADER_COMMENTS)).thenReturn(headerComment);

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
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("GroovySensor");
  }

}
