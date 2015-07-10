/*
 * Groovy :: Integration Tests
 * Copyright (C) 2012 SonarSource
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
package com.sonar.groovy.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  GroovyTest.class,
  MetricsTest.class,
  JaCoCoTest.class
})
public class Tests {

  private static final String PLUGIN_KEY = "groovy";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .addPlugin(PLUGIN_KEY)
      .addPlugin("java")
      .setMainPluginKey(PLUGIN_KEY)
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/default.xml"));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  public static String keyFor(String projectKey, String s) {
    return projectKey + ":" + (Tests.is_after_sonar_4_2() ? "src/main/groovy/" : "") + s;
  }

  public static boolean is_after_sonar_4_2() {
    return ORCHESTRATOR.getConfiguration().getSonarVersion().isGreaterThanOrEquals("4.2");
  }

  public static boolean is_after_plugin_1_0() {
    return ORCHESTRATOR.getConfiguration().getPluginVersion(PLUGIN_KEY).isGreaterThanOrEquals("1.0");
  }

  public static boolean is_after_plugin_1_1() {
    return ORCHESTRATOR.getConfiguration().getPluginVersion(PLUGIN_KEY).isGreaterThanOrEquals("1.1");
  }

  public static MavenBuild createMavenBuild() {
    MavenBuild build = MavenBuild.create();
    if (!is_multi_language()) {
      build.setProperty("sonar.language", "grvy");
    }
    return build;
  }

  private static boolean is_multi_language() {
    return is_after_plugin_1_0() && is_after_sonar_4_2();
  }

}
