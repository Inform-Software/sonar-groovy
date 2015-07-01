/*
 * Copyright (C) 2012-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
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
      .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
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
