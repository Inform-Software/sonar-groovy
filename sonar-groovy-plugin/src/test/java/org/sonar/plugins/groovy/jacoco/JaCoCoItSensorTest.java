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
import org.mockito.ArgumentMatchers;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
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

public class JaCoCoItSensorTest {

  private File jacocoExecutionData;
  private DefaultInputFile inputFile;
  private JaCoCoConfiguration configuration;
  private PathResolver pathResolver;
  private JaCoCoItSensor sensor;

  @Before
  public void setUp() throws Exception {
    File outputDir = TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/JaCoCoItSensorTests/");
    jacocoExecutionData = new File(outputDir, "jacoco-it.exec");

    FileUtils.copyFile(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "Hello.class"));
    FileUtils.copyFile(TestUtils.getResource("/org/sonar/plugins/groovy/jacoco/Hello$InnerClass.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "Hello$InnerClass.class"));

    Settings settings = new Settings();
    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, ".");

    configuration = mock(JaCoCoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    when(configuration.getItReportPath()).thenReturn(jacocoExecutionData.getPath());

    DefaultFileSystem fileSystem = new DefaultFileSystem(jacocoExecutionData.getParentFile());
    inputFile = new DefaultInputFile("", "example/Hello.groovy")
      .setLanguage(Groovy.KEY)
      .setType(Type.MAIN);
    inputFile.setLines(50);
    fileSystem.add(inputFile);

    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoItSensor(configuration, new GroovyFileSystem(fileSystem), pathResolver, settings);
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Groovy.KEY);
  }

  @Test
  public void should_Execute_On_Project_only_if_exec_exists() {
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(jacocoExecutionData);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(jacocoExecutionData.getParentFile());
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    File outputDir = TestUtils.getResource(JaCoCoSensorTest.class, ".");
    File fakeExecFile = new File(outputDir, "it.not.found.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile);
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void test_read_execution_data() {
    when(pathResolver.relativeFile(any(File.class), ArgumentMatchers.endsWith(".exec"))).thenReturn(jacocoExecutionData);

    SensorContextTester context = SensorContextTester.create(new File(""));
    sensor.execute(context);

    int[] oneHitlines = {9, 10, 25};
    int[] zeroHitlines = {14, 15, 17, 21, 29, 30, 32, 33, 38, 42, 47};
    int[] conditionLines = {14, 29, 30};

    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", CoverageType.IT, zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", CoverageType.IT, oneHitline)).isEqualTo(1);
    }
    for (int conditionLine : conditionLines) {
      assertThat(context.conditions(":example/Hello.groovy", CoverageType.IT, conditionLine)).isEqualTo(2);
      assertThat(context.coveredConditions(":example/Hello.groovy", CoverageType.IT, conditionLine)).isEqualTo(0);
    }
  }

}
