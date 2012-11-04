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

package org.sonar.plugins.groovy.cobertura;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cobertura.api.CoberturaUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaMavenPluginHandlerTest {
  CoberturaMavenPluginHandler handler;
  Settings settings;

  @Before
  public void before() {
    settings = new Settings();
    handler = new CoberturaMavenPluginHandler(settings);
  }

  @Test
  public void pluginDefinition() {
    assertThat(handler.isFixedVersion()).isFalse();
    assertThat(handler.getGroupId()).isEqualTo("org.codehaus.mojo");
    assertThat(handler.getArtifactId()).isEqualTo("cobertura-maven-plugin");
    assertThat(handler.getVersion()).isEqualTo("2.5.1");
    assertThat(handler.getGoals()).isEqualTo(new String[] {"cobertura"});
  }

  @Test
  public void setCoberturaExclusions() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[] { "**/Foo.groovy", "com/*Test.*", "com/*" });

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")).containsOnly("**/Foo.class", "com/*Test.*", "com/*.class");
  }
}
