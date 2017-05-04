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
package org.sonar.plugins.groovy.foundation;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class GroovyFileSystemTest {

  private DefaultFileSystem fileSystem;
  private GroovyFileSystem groovyFileSystem;

  @Before
  public void setUp() {
    fileSystem = new DefaultFileSystem(new File("."));
    groovyFileSystem = new GroovyFileSystem(fileSystem);
  }

  @Test
  public void isEnabled() {
    assertThat(groovyFileSystem.hasGroovyFiles()).isFalse();

    fileSystem.add(new DefaultInputFile("", "fake.file"));
    assertThat(groovyFileSystem.hasGroovyFiles()).isFalse();

    fileSystem.add(new DefaultInputFile("", "fake.groovy").setLanguage(Groovy.KEY));
    assertThat(groovyFileSystem.hasGroovyFiles()).isTrue();
  }

  @Test
  public void getSourceFile() {
    assertThat(groovyFileSystem.sourceFiles()).isEmpty();

    fileSystem.add(new DefaultInputFile("", "fake.file"));
    assertThat(groovyFileSystem.sourceFiles()).isEmpty();

    fileSystem.add(new DefaultInputFile("", "fake.groovy").setLanguage(Groovy.KEY));
    assertThat(groovyFileSystem.sourceFiles()).hasSize(1);
  }

  @Test
  public void inputFileFromRelativePath() {
    assertThat(groovyFileSystem.sourceInputFileFromRelativePath(null)).isNull();

    fileSystem.add(new DefaultInputFile("", "fake1.file"));
    assertThat(groovyFileSystem.sourceInputFileFromRelativePath("fake1.file")).isNull();

    fileSystem.add(new DefaultInputFile("", "fake2.file").setType(Type.MAIN).setLanguage(Groovy.KEY));
    assertThat(groovyFileSystem.sourceInputFileFromRelativePath("fake2.file")).isNotNull();

    fileSystem.add(new DefaultInputFile("", "org/sample/foo/fake3.file").setType(Type.MAIN).setLanguage(Groovy.KEY));
    assertThat(groovyFileSystem.sourceInputFileFromRelativePath("foo/fake3.file")).isNotNull();
  }
}
