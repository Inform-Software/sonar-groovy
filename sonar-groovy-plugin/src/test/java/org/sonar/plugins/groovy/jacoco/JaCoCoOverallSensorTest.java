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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.TestUtils;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

public class JaCoCoOverallSensorTest {

  @Rule public final TemporaryFolder tmpDir = new TemporaryFolder();

  private JaCoCoSensor sensor;
  private MapSettings settings = new MapSettings();

  @Before
  public void before() throws IOException {
    Path outputDir = tmpDir.newFolder().toPath();

    Files.copy(
        TestUtils.getResource(getClass(), "../JaCoCoSensor_0_7_5/jacoco-ut.exec"),
        outputDir.resolve("jacoco-ut.exec"));
    Files.copy(
        TestUtils.getResource(getClass(), "../JaCoCoSensor_0_7_5/jacoco-ut.exec"),
        outputDir.resolve("jacoco-it.exec"));
    Files.copy(
        TestUtils.getResource(getClass(), "../Hello.class.toCopy"),
        outputDir.resolve("Hello.class"));
    Files.copy(
        TestUtils.getResource(getClass(), "../Hello$InnerClass.class.toCopy"),
        outputDir.resolve("Hello$InnerClass.class"));

    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, ".");

    SensorContextTester context = SensorContextTester.create(outputDir);
    context.fileSystem().setWorkDir(tmpDir.newFolder().toPath());

    InputFile inputFile =
        TestInputFileBuilder.create("", "example/Hello.groovy")
            .setLanguage(Groovy.KEY)
            .setType(Type.MAIN)
            .setLines(50)
            .build();
    context.fileSystem().add(inputFile);

    JaCoCoConfiguration configuration = new JaCoCoConfiguration(settings, context.fileSystem());
    sensor =
        new JaCoCoSensor(
            configuration,
            new GroovyFileSystem(context.fileSystem()),
            new PathResolver(),
            settings);
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Groovy.KEY);
  }

  @Test
  public void should_Execute_On_Project_only_if_at_least_one_exec_exists() {
    settings.setProperty(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY, "jacoco-it.exec");
    settings.setProperty(JaCoCoConfiguration.REPORT_PATH_PROPERTY, "notexist.exec");
    configReports(false, true);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    configReports(true, false);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    settings.setProperty(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY, "notexist.exec");
    settings.setProperty(JaCoCoConfiguration.REPORT_PATH_PROPERTY, "notexist.exec");
    assertThat(sensor.shouldExecuteOnProject()).isFalse();
  }

  private void configReports(boolean utReport, boolean itReport) {
    settings.setProperty(
        JaCoCoConfiguration.REPORT_PATH_PROPERTY, utReport ? "jacoco-ut.exec" : "notexist-ut.exec");
    settings.setProperty(
        JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY,
        itReport ? "jacoco-it.exec" : "notexist-it.exec");
  }
}
