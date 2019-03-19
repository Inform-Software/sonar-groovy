/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2019 SonarSource SA & Community
 * mailto:info AT sonarsource DOT com
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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.TestUtils;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

public class JaCoCoItSensorTest {

  @Rule public final TemporaryFolder tmpDir = new TemporaryFolder();

  private Path outputDir;
  private InputFile inputFile;
  private MapSettings settings = new MapSettings();
  private JaCoCoConfiguration configuration;
  private JaCoCoItSensor sensor;

  @Before
  public void setUp() throws Exception {
    outputDir = tmpDir.newFolder().toPath();
    Files.copy(
        TestUtils.getResource(getClass(), "../JaCoCoItSensorTests/jacoco-it.exec"),
        outputDir.resolve("jacoco-it.exec"));
    Files.copy(
        TestUtils.getResource(getClass(), "../Hello.class.toCopy"),
        outputDir.resolve("Hello.class"));
    Files.copy(
        TestUtils.getResource(getClass(), "../Hello$InnerClass.class.toCopy"),
        outputDir.resolve("Hello$InnerClass.class"));

    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, ".");
    settings.setProperty(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY, "jacoco-it.exec");

    DefaultFileSystem fileSystem = new DefaultFileSystem(outputDir);
    inputFile =
        TestInputFileBuilder.create("", "example/Hello.groovy")
            .setLanguage(Groovy.KEY)
            .setType(Type.MAIN)
            .setLines(50)
            .build();
    fileSystem.add(inputFile);
    configuration = new JaCoCoConfiguration(settings, fileSystem);

    sensor =
        new JaCoCoItSensor(
            configuration, new GroovyFileSystem(fileSystem), new PathResolver(), settings);
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Groovy.KEY);
  }

  @Test
  public void should_Execute_On_Project_only_if_exec_exists() {
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    settings.setProperty(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY, ".");
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    settings.setProperty(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY, "it.not.found.exec");
    assertThat(sensor.shouldExecuteOnProject()).isFalse();
  }

  @Test
  public void test_read_execution_data() {
    SensorContextTester context = SensorContextTester.create(Paths.get("."));
    sensor.execute(context);

    int[] oneHitlines = {9, 10, 25};
    int[] zeroHitlines = {14, 15, 17, 21, 29, 30, 32, 33, 38, 42, 47};
    int[] conditionLines = {14, 29, 30};

    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", oneHitline)).isEqualTo(1);
    }
    for (int conditionLine : conditionLines) {
      assertThat(context.conditions(":example/Hello.groovy", conditionLine)).isEqualTo(2);
      assertThat(context.coveredConditions(":example/Hello.groovy", conditionLine)).isEqualTo(0);
    }
  }
}
