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
package org.sonar.plugins.groovy.foundation;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.plugins.groovy.GroovyPlugin;

import static org.fest.assertions.Assertions.assertThat;

public class GroovyTest {

  @Test
  public void test() {
    Settings settings = new Settings();
    Groovy language = new Groovy(settings);
    assertThat(language.getKey()).isEqualTo("grvy");
    assertThat(language.getName()).isEqualTo("Groovy");
    assertThat(language.getFileSuffixes()).isEqualTo(new String[] {".groovy"});

    settings.setProperty(GroovyPlugin.FILE_SUFFIXES_KEY, "");
    assertThat(language.getFileSuffixes()).containsOnly(".groovy");

    settings.setProperty(GroovyPlugin.FILE_SUFFIXES_KEY, ".groovy, .grvy");
    assertThat(language.getFileSuffixes()).containsOnly(".groovy", ".grvy");
  }

  @Test
  public void binaryDirectories() throws Exception {
    Settings settings = new Settings();
    Groovy language = new Groovy(settings);

    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, "");
    assertThat(language.getBinaryDirectories()).isEmpty();

    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, "target/firstDir , target/secondDir");
    assertThat(language.getBinaryDirectories()).hasSize(2);

    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES, "");
    // property 'sonar.binaries' is set by maven and gradle plugins
    settings.setProperty(GroovyPlugin.SONAR_GROOVY_BINARIES_FALLBACK, "target/firstDir , target/secondDir");
    assertThat(language.getBinaryDirectories()).hasSize(2);
  }
}
