/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2020 SonarSource SA & Community
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
package org.sonar.plugins.groovy.surefire.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.TestUtils;

public class SurefireUtilsTest {

  private FileSystem fs =
      new DefaultFileSystem(TestUtils.getResource(getClass(), "shouldGetReportsFromProperty"));
  private PathResolver pathResolver = new PathResolver();

  @Test
  public void should_get_reports_from_property() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.junit.reportsPath", "target/surefire");
    assertThat(SurefireUtils.getReportsDirectory(settings, fs, pathResolver).exists()).isTrue();
    assertThat(SurefireUtils.getReportsDirectory(settings, fs, pathResolver).isDirectory())
        .isTrue();
  }

  @Test
  public void return_default_value_if_property_unset() throws Exception {
    File directory = SurefireUtils.getReportsDirectory(new MapSettings(), fs, pathResolver);
    assertThat(directory.getCanonicalPath())
        .endsWith("target" + File.separator + "surefire-reports");
    assertThat(directory.exists()).isFalse();
    assertThat(directory.isDirectory()).isFalse();
  }

  @Test
  public void return_default_value_if_can_not_read_file() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.junit.reportsPath", "../target/\u0000:surefire");
    File directory = SurefireUtils.getReportsDirectory(settings, fs, pathResolver);
    assertThat(directory.getCanonicalPath())
        .endsWith("target" + File.separator + "surefire-reports");
    assertThat(directory.exists()).isFalse();
    assertThat(directory.isDirectory()).isFalse();
  }
}
