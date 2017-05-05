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

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.TestUtils;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JaCoCoOverallSensorTest {

  private JaCoCoConfiguration configuration;
  private PathResolver pathResolver;
  private JaCoCoOverallSensor sensor;
  private File jacocoUTData;
  private File jacocoITData;
  private File outputDir;
  private DefaultInputFile inputFile;
  private Settings settings;
  private SensorContextTester context;

  @Before
  public void before() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/JaCoCoOverallSensorTests/");
    jacocoUTData = new File(outputDir, "jacoco-ut.exec");
    jacocoITData = new File(outputDir, "jacoco-it.exec");

    FileUtils.copyFile(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello.class.toCopy"),
      new File(jacocoUTData.getParentFile(), "Hello.class"));
    FileUtils.copyFile(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello$InnerClass.class.toCopy"),
      new File(jacocoUTData.getParentFile(), "Hello$InnerClass.class"));

    settings = new Settings();
    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, ".");

    context = SensorContextTester.create(jacocoUTData.getParentFile());

    context.fileSystem().setWorkDir(jacocoUTData.getParentFile());
    inputFile = new DefaultInputFile("", "example/Hello.groovy")
      .setLanguage(Groovy.KEY)
      .setType(Type.MAIN);
    inputFile.setLines(50);
    context.fileSystem().add(inputFile);

    configuration = mock(JaCoCoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoOverallSensor(configuration, new GroovyFileSystem(context.fileSystem()), pathResolver, settings);
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Groovy.KEY);
  }

  @Test
  public void should_Execute_On_Project_only_if_at_least_one_exec_exists() {
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(jacocoITData);
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile());
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile());
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(jacocoUTData);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile());
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile());
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void test_read_execution_data_with_IT_and_UT() {
    setMocks(true, true);

    sensor.execute(context);

    int[] oneHitlines = {9, 10, 14, 15, 17, 21, 25, 29, 32, 33, 42, 47};
    int[] zeroHitlines = {30, 38};
    int[] conditionLines = {14, 29, 30};
    int[] coveredConditions = {2, 1, 0};

    verifyOverallMetrics(context, zeroHitlines, oneHitlines, conditionLines, coveredConditions);
  }

  private void verifyOverallMetrics(SensorContextTester context, int[] zeroHitlines, int[] oneHitlines, int[] conditionLines, int[] coveredConditions) {
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(inputFile.key(), CoverageType.OVERALL, zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(inputFile.key(), CoverageType.OVERALL, oneHitline)).isEqualTo(1);
    }

    for (int i = 0; i < conditionLines.length; i++) {
      int line = conditionLines[i];
      assertThat(context.conditions(inputFile.key(), CoverageType.OVERALL, line)).isEqualTo(2);
      assertThat(context.coveredConditions(inputFile.key(), CoverageType.OVERALL, line)).isEqualTo(coveredConditions[i]);
    }
  }

  @Test
  public void test_read_execution_data_with_IT_and_UT_and_binaryDirs_being_absolute() {
    setMocks(true, true);
    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, jacocoUTData.getParentFile().getAbsolutePath());

    sensor.execute(context);

    int[] oneHitlines = {9, 10, 14, 15, 17, 21, 25, 29, 32, 33, 42, 47};
    int[] zeroHitlines = {30, 38};
    int[] conditionLines = {14, 29, 30};
    int[] coveredConditions = {2, 1, 0};

    verifyOverallMetrics(context, zeroHitlines, oneHitlines, conditionLines, coveredConditions);
  }

  @Test
  public void test_read_execution_data_with_only_UT() {
    setMocks(true, false);

    sensor.execute(context);

    int[] oneHitlines = {9, 10, 14, 15, 17, 21, /* 25 not covered in UT */ 29, 32, 33, 42, 47};
    int[] zeroHitlines = {25, 30, 38};
    int[] conditionLines = {14, 29, 30};
    int[] coveredConditions = {2, 1, 0};

    verifyOverallMetrics(context, zeroHitlines, oneHitlines, conditionLines, coveredConditions);
  }

  @Test
  public void test_read_execution_data_with_only_IT() {
    setMocks(false, true);

    sensor.execute(context);

    int[] oneHitlines = {9, 10, 25};
    int[] zeroHitlines = {14, 15, 17, 21, 29, 30, 32, 33, 38, 42, 47};
    int[] conditionLines = {14, 29, 30};
    int[] coveredConditions = {0, 0, 0};

    verifyOverallMetrics(context, zeroHitlines, oneHitlines, conditionLines, coveredConditions);
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
