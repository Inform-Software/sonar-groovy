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
package org.sonar.plugins.groovy;

import static org.assertj.core.api.Assertions.assertThat;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

public class GroovyPluginTest {
  public static final Version VERSION_6_7 = Version.create(6, 7);

  @Test
  public void testExtensions() {
    GroovyPlugin plugin = new GroovyPlugin();

    Binding b = new Binding();
    Class<?> edition = null;
    String call = "rt.forSonarQube(ver, scanner)";
    try {
      edition = Class.forName("org.sonar.api.SonarEdition");
      call = "rt.forSonarQube(ver, scanner, ed.COMMUNITY)";
    } catch (ClassNotFoundException e) {
      // SKIP on old SonarQube
    }
    b.setVariable("ver", VERSION_6_7);
    b.setVariable("scanner", SonarQubeSide.SCANNER);
    b.setVariable("ed", edition);
    b.setVariable("rt", SonarRuntimeImpl.class);
    GroovyShell sh = new GroovyShell(b);

    SonarRuntime runtime = (SonarRuntime) sh.evaluate(call);
    Plugin.Context context = new Plugin.Context(runtime);
    plugin.define(context);
    assertThat(context.getExtensions()).hasSize(14);
  }
}
