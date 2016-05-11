/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.groovy.jacoco;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoSensorTest {

  private File jacocoExecutionData;
  private InputFile inputFile;
  private JaCoCoConfiguration configuration;
  private SensorContext context;
  private PathResolver pathResolver;
  private Project project;
  private JaCoCoSensor sensor;

  @Before
  public void setUp() throws Exception {
    this.jacocoExecutionData = initWithJaCoCoVersion("JaCoCoSensor_0_7_4");
  }

  private File initWithJaCoCoVersion(String jacocoVersion) throws IOException {
    File outputDir = TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/" + jacocoVersion + "/");
    File jacocoExecutionData = new File(outputDir, "jacoco-ut.exec");

    Files.copy(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "Hello.class"));
    Files.copy(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello$InnerClass.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "Hello$InnerClass.class"));

    Groovy groovy = mock(Groovy.class);
    when(groovy.getBinaryDirectories()).thenReturn(Lists.newArrayList("."));

    configuration = mock(JaCoCoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    when(configuration.getReportPath()).thenReturn(jacocoExecutionData.getPath());

    DefaultFileSystem fileSystem = new DefaultFileSystem(jacocoExecutionData.getParentFile());
    inputFile = new DefaultInputFile("example/Hello.groovy")
      .setLanguage(Groovy.KEY)
      .setType(Type.MAIN)
      .setAbsolutePath(fileSystem.baseDir() + "/example/Hello.groovy");
    fileSystem.add(inputFile);

    context = mock(SensorContext.class);
    pathResolver = mock(PathResolver.class);
    project = mock(Project.class);
    sensor = new JaCoCoSensor(groovy, configuration, fileSystem, pathResolver);

    return jacocoExecutionData;
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("Groovy JaCoCoSensor");
  }

  @Test
  public void should_depend_on_surefire() {
    assertThat(sensor.dependsUponSurefireSensors()).isEqualTo("surefire-java");
  }

  @Test
  public void should_Execute_On_Project_only_if_exec_exists() {
    when(configuration.getReportPath()).thenReturn("ut.exec");

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(jacocoExecutionData);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(jacocoExecutionData.getParentFile());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    File outputDir = TestUtils.getResource(JaCoCoSensorTest.class, ".");
    File fakeExecFile = new File(outputDir, "ut.not.found.exec");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void test_read_execution_data_with_jacoco_0_7_4() {
    when(pathResolver.relativeFile(any(File.class), argThat(Matchers.endsWith(".exec")))).thenReturn(jacocoExecutionData);

    sensor.analyse(project, context);

    verifyMeasures();
  }

  @Test
  public void test_read_execution_data_with_jacoco_0_7_5() throws IOException {
    File jacocoExecutionData = initWithJaCoCoVersion("JaCoCoSensor_0_7_5");
    when(pathResolver.relativeFile(any(File.class), argThat(Matchers.endsWith(".exec")))).thenReturn(jacocoExecutionData);

    sensor.analyse(project, context);

    verifyMeasures();
  }

  private void verifyMeasures() {
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 14.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "9=1;10=1;14=1;15=1;17=1;21=1;25=0;29=1;30=0;32=1;33=1;38=0;42=1;47=1")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 3.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.CONDITIONS_BY_LINE, "14=2;29=2;30=2")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "14=2;29=1;30=0")));
  }
}
