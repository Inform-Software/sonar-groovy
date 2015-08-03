/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.groovy.jacoco;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoOverallSensorTest {

  private JaCoCoConfiguration configuration;
  private SensorContext context;
  private ModuleFileSystem fileSystem;
  private PathResolver pathResolver;
  private Project project;
  private JaCoCoOverallSensor sensor;
  private File jacocoUTData;
  private File jacocoITData;
  private File outputDir;
  private InputFile inputFile;

  @Before
  public void before() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/JaCoCoOverallSensorTests/");
    jacocoUTData = new File(outputDir, "jacoco-ut.exec");
    jacocoITData = new File(outputDir, "jacoco-it.exec");

    Files.copy(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello.class.toCopy"),
      new File(jacocoUTData.getParentFile(), "Hello.class"));
    Files.copy(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello$InnerClass.class.toCopy"),
      new File(jacocoUTData.getParentFile(), "Hello$InnerClass.class"));
    List<File> binaryDirs = Lists.newArrayList(jacocoUTData.getParentFile());

    ModuleFileSystem moduleFileSystem = mock(ModuleFileSystem.class);
    when(moduleFileSystem.binaryDirs()).thenReturn(binaryDirs);

    DefaultFileSystem fileSystem = new DefaultFileSystem(jacocoUTData.getParentFile());
    fileSystem.setWorkDir(jacocoUTData.getParentFile());
    inputFile = new DefaultInputFile("org/sonar/plugins/groovy/jacoco/tests/Hello.groovy")
      .setLanguage(Groovy.KEY)
      .setType(Type.MAIN)
      .setAbsolutePath(fileSystem.baseDir() + "/org/sonar/plugins/groovy/jacoco/tests/Hello.groovy");
    fileSystem.add(inputFile);

    configuration = mock(JaCoCoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    context = mock(SensorContext.class);
    pathResolver = mock(PathResolver.class);
    project = mock(Project.class);
    sensor = new JaCoCoOverallSensor(configuration, moduleFileSystem, fileSystem, pathResolver);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoOverallSensor");
  }

  @Test
  public void should_Execute_On_Project_only_if_at_least_one_exec_exists() {
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(jacocoITData);
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile());
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile());
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(jacocoUTData);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile());
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void test_read_execution_data_with_IT_and_UT() {
    setMocks(true, true);

    sensor.analyse(project, context);

    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 14.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 2.0)));
    verify(context).saveMeasure(eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "9=1;10=1;14=1;15=1;17=1;21=1;25=1;29=1;30=0;32=1;33=1;38=0;42=1;47=1")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 3.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "14=2;29=2;30=2")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, "14=2;29=1;30=0")));
  }

  @Test
  public void test_read_execution_data_with_only_UT() {
    setMocks(true, false);

    sensor.analyse(project, context);

    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 14.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "9=1;10=1;14=1;15=1;17=1;21=1;25=0;29=1;30=0;32=1;33=1;38=0;42=1;47=1")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 3.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "14=2;29=2;30=2")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, "14=2;29=1;30=0")));
  }

  @Test
  public void test_read_execution_data_with_only_IT() {
    setMocks(false, true);

    sensor.analyse(project, context);

    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 14.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 11.0)));
    verify(context).saveMeasure(eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "9=1;10=1;14=0;15=0;17=0;21=0;25=1;29=0;30=0;32=0;33=0;38=0;42=0;47=0")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 6.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "14=2;29=2;30=2")));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, "14=0;29=0;30=0")));
  }

  private void setMocks(boolean utReport, boolean itReport) {
    when(configuration.getReportPath()).thenReturn("ut.exec");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(utReport ? jacocoUTData : fakeExecFile());
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(itReport ? jacocoITData : fakeExecFile());
    File jacocoOverallData = new File(outputDir, "jacoco-overall.exec");
    when(pathResolver.relativeFile(any(File.class), eq(jacocoOverallData.getAbsolutePath()))).thenReturn(jacocoOverallData);
  }

  private File fakeExecFile() {
    return new File("fake.exec");
  }
}
