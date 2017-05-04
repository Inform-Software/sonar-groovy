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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class JaCoCoConfigurationTest {

  private Settings settings;
  private JaCoCoConfiguration jacocoSettings;
  private DefaultFileSystem fileSystem;

  @Before
  public void setUp() {
    settings = new Settings(new PropertyDefinitions().addComponents(JaCoCoConfiguration.getPropertyDefinitions()));
    fileSystem = new DefaultFileSystem(new File("."));
    jacocoSettings = new JaCoCoConfiguration(settings, fileSystem);
  }

  @Test
  public void shouldExecuteOnProject() throws Exception {
    // no files
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isFalse();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isFalse();

    fileSystem.add(new DefaultInputFile("", "src/foo/bar.java").setLanguage("java"));
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isFalse();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isFalse();

    fileSystem.add(new DefaultInputFile("", "src/foo/bar.groovy").setLanguage(Groovy.KEY));
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isTrue();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isFalse();

    settings.setProperty(JaCoCoConfiguration.REPORT_MISSING_FORCE_ZERO, true);
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isTrue();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isTrue();
  }

  @Test
  public void defaults() {
    assertThat(jacocoSettings.getReportPath()).isEqualTo("target/jacoco.exec");
    assertThat(jacocoSettings.getItReportPath()).isEqualTo("target/jacoco-it.exec");
  }

  @Test
  public void shouldReturnItReportPathWhenModified() {
    settings.setProperty(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY, "target/it-jacoco-test.exec");
    assertThat(jacocoSettings.getItReportPath()).isEqualTo("target/it-jacoco-test.exec");
  }

  @Test
  public void shouldReturnReportPathWhenModified() {
    settings.setProperty(JaCoCoConfiguration.REPORT_PATH_PROPERTY, "jacoco.exec");
    assertThat(jacocoSettings.getReportPath()).isEqualTo("jacoco.exec");
  }
}
