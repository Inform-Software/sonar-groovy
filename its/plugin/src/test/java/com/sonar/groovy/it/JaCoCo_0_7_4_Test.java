/*
 * Groovy :: Integration Tests
 * Copyright (C) 2012-2016 SonarSource SA
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
package com.sonar.groovy.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class JaCoCo_0_7_4_Test {

  private static final String PROJECT = "org.sonarsource.it.groovy.samples:JaCoCo-Integration";
  private static final String PACKAGE_TESTS = Tests.keyFor(PROJECT, "org/sonar/plugins/groovy/jacoco/tests");
  private static final String FILE_HELLO = Tests.keyFor(PROJECT, "org/sonar/plugins/groovy/jacoco/tests/Hello.groovy");

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  private static BuildResult buildResult;

  @BeforeClass
  public static void init() {
    MavenBuild build = Tests.createMavenBuild()
      .setGoals("clean install sonar:sonar")
      .setPom(FileLocation.of("projects/jacoco-integration/pom.xml"))
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.groovy.jacoco.reportPath", "target/coverage-reports/jacoco-ut.exec")
      .setProperty("sonar.groovy.jacoco.itReportPath", "target/coverage-reports/jacoco-it.exec")
      .setProperty("sonar.jacoco.reportPath", "target/coverage-reports/jacoco-ut.exec")
      .setProperty("sonar.jacoco.itReportPath", "target/coverage-reports/jacoco-it.exec")
      .setProperty("jacoco.version", "0.7.4.201502262128");
    buildResult = orchestrator.executeBuild(build);
  }

  @Test
  public void project_is_analyzed() {
    Sonar client = orchestrator.getServer().getWsClient();
    assertThat(client.find(new ResourceQuery(PROJECT)).getName()).isEqualTo("JaCoCo-Integration");
    assertThat(client.find(new ResourceQuery(PROJECT)).getVersion()).isEqualTo("1.0-SNAPSHOT");
  }

  @Test
  public void jacoco_report_should_be_read_when_analyzing_with_sonar_runner() {
    assertThat(buildResult.getLogs()).doesNotContain("JaCoCo reports not found.");
  }

  @Test
  public void unit_test_coverage_metrics() {
    assertThat(getProjectMeasureValue("coverage")).isEqualTo(73.1, Delta.delta(0.1));
    assertThat(getPackageMeasureValue("coverage")).isEqualTo(73.1, Delta.delta(0.1));
    assertThat(getProjectMeasureValue("line_coverage")).isEqualTo(83.3);
    assertThat(getProjectMeasureValue("branch_coverage")).isEqualTo(50.0);

    assertThat(getFileMeasureValue("line_coverage")).isEqualTo(78.6);
    assertThat(getFileMeasureValue("branch_coverage")).isEqualTo(50.0);
  }

  @Test
  public void integration_test_coverage_metrics() {
    assertThat(getProjectMeasureValue("it_coverage")).isEqualTo(30.8, Delta.delta(0.1));
    assertThat(getPackageMeasureValue("it_coverage")).isEqualTo(30.8, Delta.delta(0.1));
    assertThat(getProjectMeasureValue("it_line_coverage")).isEqualTo(38.9);
    assertThat(getProjectMeasureValue("it_branch_coverage")).isEqualTo(12.5);

    assertThat(getFileMeasureValue("it_line_coverage")).isEqualTo(21.4);
    assertThat(getFileMeasureValue("it_branch_coverage")).isEqualTo(0.0);
  }

  @Test
  public void overall_coverage_metrics() {
    assertThat(getProjectMeasureValue("overall_coverage")).isEqualTo(80.8, Delta.delta(0.1));
    assertThat(getPackageMeasureValue("overall_coverage")).isEqualTo(80.8, Delta.delta(0.1));
    assertThat(getProjectMeasureValue("overall_line_coverage")).isEqualTo(88.9);
    assertThat(getProjectMeasureValue("overall_branch_coverage")).isEqualTo(62.5);

    assertThat(getFileMeasureValue("overall_line_coverage")).isEqualTo(85.7);
    assertThat(getFileMeasureValue("overall_branch_coverage")).isEqualTo(50.0);
  }

  private static Double getProjectMeasureValue(String metricKey) {
    return getValue(orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKey)).getMeasure(metricKey));
  }

  private static Double getPackageMeasureValue(String metricKey) {
    return getValue(orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PACKAGE_TESTS, metricKey)).getMeasure(metricKey));
  }

  private static Double getFileMeasureValue(String metricKey) {
    return getValue(orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(FILE_HELLO, metricKey)).getMeasure(metricKey));
  }

  private static Double getValue(Measure measure) {
    if (measure != null) {
      return measure.getValue();
    }
    return null;
  }
}
