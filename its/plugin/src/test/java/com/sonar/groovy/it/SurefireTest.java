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
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class SurefireTest {

  private static final String PROJECT = "org.sonarsource.it.groovy.samples:surefire";

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    MavenBuild build = Tests.createMavenBuild()
      .setGoals("clean install sonar:sonar")
      .setPom(FileLocation.of("projects/surefire/pom.xml"))
      .setProperty("sonar.scm.disabled", "true");
    orchestrator.executeBuild(build);
  }

  @Test
  public void project_is_analyzed() {
    Sonar client = orchestrator.getServer().getWsClient();
    assertThat(client.find(new ResourceQuery(PROJECT)).getName()).isEqualTo("Groovy-Surefire");
    assertThat(client.find(new ResourceQuery(PROJECT)).getVersion()).isEqualTo("1.0-SNAPSHOT");
  }

  @Test
  public void test_should_have_been_integrated() {
    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(PROJECT, "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density"));

    if (Tests.is_after_plugin_1_2()) {
      assertThat(project.getMeasure("tests").getIntValue()).isEqualTo(1);
      assertThat(project.getMeasure("test_errors").getIntValue()).isEqualTo(0);
      assertThat(project.getMeasure("test_failures").getIntValue()).isEqualTo(0);
      assertThat(project.getMeasure("skipped_tests").getIntValue()).isEqualTo(0);
      assertThat(project.getMeasure("test_execution_time").getIntValue()).isGreaterThan(0);
      assertThat(project.getMeasure("test_success_density").getValue()).isEqualTo(100.0);
    }
  }
}
