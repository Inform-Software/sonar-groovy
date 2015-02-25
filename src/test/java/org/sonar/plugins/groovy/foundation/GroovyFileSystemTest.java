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
package org.sonar.plugins.groovy.foundation;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import static org.fest.assertions.Assertions.assertThat;

public class GroovyFileSystemTest {
  @Test
  public void isEnabled() {
    DefaultFileSystem fileSystem = new DefaultFileSystem();
    assertThat(GroovyFileSystem.hasGroovyFiles(fileSystem)).isFalse();

    fileSystem.add(new DefaultInputFile("fake.file"));
    assertThat(GroovyFileSystem.hasGroovyFiles(fileSystem)).isFalse();

    fileSystem.add(new DefaultInputFile("fake.groovy").setLanguage(Groovy.KEY));
    assertThat(GroovyFileSystem.hasGroovyFiles(fileSystem)).isTrue();
  }

  @Test
  public void getSourceFile() {
    DefaultFileSystem fileSystem = new DefaultFileSystem();
    assertThat(GroovyFileSystem.sourceFiles(fileSystem)).isEmpty();

    fileSystem.add(new DefaultInputFile("fake.file"));
    assertThat(GroovyFileSystem.sourceFiles(fileSystem)).isEmpty();

    fileSystem.add(new DefaultInputFile("fake.groovy").setLanguage(Groovy.KEY).setAbsolutePath("fake.groovy"));
    assertThat(GroovyFileSystem.sourceFiles(fileSystem)).hasSize(1);
  }
}
