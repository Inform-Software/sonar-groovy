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
import org.fest.assertions.Delta;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class GroovyTest {

  @ClassRule
  public static final Orchestrator orchestrator = Tests.ORCHESTRATOR;

  private static final String PROJECT_CODENARC = "org.codenarc:CodeNarc";
  private static final String PACKAGE_REPORT = Tests.keyFor(PROJECT_CODENARC, "org/codenarc/report");
  private static final String FILE_REPORT_WRITER = Tests.keyFor(PROJECT_CODENARC, "org/codenarc/report/HtmlReportWriter.groovy");

  @BeforeClass
  public static void inspect_codenarc() {
    // Usual Maven build to generate coverage:
    orchestrator.executeBuild(MavenBuild.create()
      .setPom(FileLocation.of("projects/codenarc-0.9-r1/pom.xml"))
      .setProperty("cobertura.report.format", "xml")
      .setGoals("clean", "org.codehaus.mojo:cobertura-maven-plugin:2.5:cobertura"));

    // Sonar analysis:
    MavenBuild build = Tests.createMavenBuild()
      .setPom(FileLocation.of("projects/codenarc-0.9-r1/pom.xml"))
      .setProfile("default")
      .setProperty("sonar.groovy.ignoreHeaderComments", "true")
      .setProperty("sonar.scm.disabled", "true")
      .setGoals("sonar:sonar");

    if (Tests.is_after_plugin_1_0()) {
      build.setProperty("sonar.dynamicAnalysis", "reuseReports")
        .setProperty("sonar.groovy.cobertura.reportPath", "target/site/cobertura/coverage.xml");
    } else {
      build.setProperty("sonar.dynamicAnalysis", "false");
    }

    orchestrator.executeBuild(build);
  }

  @Test
  public void codenarc_is_analyzed() {
    Sonar client = orchestrator.getServer().getWsClient();
    assertThat(client.find(new ResourceQuery(PROJECT_CODENARC)).getName()).isEqualTo("CodeNarc");
    assertThat(client.find(new ResourceQuery(PROJECT_CODENARC)).getVersion()).isEqualTo("0.9");
  }

  /*
   * ====================== PROJECT LEVEL ======================
   */

  @Test
  public void projectsMetrics() {
    assertThat(getProjectMeasure("ncloc").getIntValue()).isEqualTo(4199);
    assertThat(getProjectMeasure("files").getIntValue()).isEqualTo(135);
    assertThat(getProjectMeasure("classes").getIntValue()).isEqualTo(196);
    // Package calculation by Groovy plugin prior to 1.0 is not compatible with sonar 4.2
    if (!Tests.is_after_sonar_4_2()) {
      assertThat(getProjectMeasure("packages").getIntValue()).isEqualTo(22);
    }
    assertThat(getProjectMeasure("functions").getIntValue()).isEqualTo(406);

    if (Tests.is_after_plugin_1_1()) {
      assertThat(getProjectMeasure("violations").getIntValue()).isEqualTo(13);
    } else {
      assertThat(getProjectMeasure("violations").getIntValue()).isEqualTo(12);
    }
    if (Tests.is_after_plugin_1_0()) {
      assertThat(getProjectMeasure("lines").getIntValue()).isEqualTo(9458);
      assertThat(getProjectMeasure("comment_lines").getIntValue()).isEqualTo(2325);
      assertThat(getProjectMeasure("complexity").getIntValue()).isEqualTo(873);
      assertThat(getProjectMeasure("function_complexity").getValue()).isEqualTo(2.2);
      assertThat(getProjectMeasure("class_complexity").getValue()).isEqualTo(4.5);
      assertThat(getProjectMeasure("comment_lines_density").getValue()).isEqualTo(35.6);
      assertThat(getProjectMeasure("function_complexity_distribution").getData()).isEqualTo("1=212;2=134;4=43;6=11;8=1;10=4;12=1");
    } else {
      assertThat(getProjectMeasure("lines").getIntValue()).isEqualTo(9439);
      assertThat(getProjectMeasure("comment_lines").getIntValue()).isEqualTo(2856);
      assertThat(getProjectMeasure("comment_lines_density").getValue()).isEqualTo(40.5);
      assertThat(getProjectMeasure("complexity").getIntValue()).isEqualTo(870);
      assertThat(getProjectMeasure("function_complexity").getValue()).isEqualTo(2.1);
      assertThat(getProjectMeasure("class_complexity").getValue()).isEqualTo(4.4);
      assertThat(getProjectMeasure("function_complexity_distribution").getData()).isEqualTo("1=212;2=137;4=40;6=11;8=1;10=4;12=1");
    }
    assertThat(getProjectMeasure("file_complexity_distribution").getData()).isEqualTo("0=73;5=32;10=21;20=7;30=2;60=0;90=0");
    assertThat(getProjectMeasure("class_complexity_distribution")).isNull();
  }

  /**
   * SONAR-3139
   */
  @Test
  public void project_duplications() {
    assertThat(getProjectMeasure("duplicated_files").getIntValue()).isEqualTo(2);
    assertThat(getProjectMeasure("duplicated_lines").getIntValue()).isEqualTo(50);
    assertThat(getProjectMeasure("duplicated_blocks").getIntValue()).isEqualTo(2);
    assertThat(getProjectMeasure("duplicated_lines_density").getValue()).isEqualTo(0.5);
  }

  @Test
  public void testProjectCoverage() {
    Assume.assumeTrue(Tests.is_after_plugin_1_0());

    // We are getting different results for different Java versions : 1.6.0_21 and 1.5.0_16
    assertThat(getProjectMeasure("coverage").getValue()).isEqualTo(89.5, Delta.delta(0.2));
    assertThat(getProjectMeasure("line_coverage").getValue()).isEqualTo(98.8, Delta.delta(0.2));
    assertThat(getProjectMeasure("lines_to_cover").getValue()).isEqualTo(1668.0, Delta.delta(10.0));
    assertThat(getProjectMeasure("uncovered_lines").getValue()).isEqualTo(20.0, Delta.delta(2.0));

    assertThat(getProjectMeasure("tests")).isNull();
    assertThat(getProjectMeasure("test_success_density")).isNull();
  }

  /*
   * ====================== DIRECTORY LEVEL ======================
   */

  @Test
  public void packagesMetrics() {
    assertThat(getPackageMeasure("ncloc").getIntValue()).isEqualTo(540);
    assertThat(getPackageMeasure("lines").getIntValue()).isEqualTo(807);
    assertThat(getPackageMeasure("files").getIntValue()).isEqualTo(6);
    assertThat(getPackageMeasure("classes").getIntValue()).isEqualTo(6);
    // Package calculation by Groovy plugin prior to 1.0 is not compatible with sonar 4.2
    if (!Tests.is_after_sonar_4_2()) {
      assertThat(getPackageMeasure("packages").getIntValue()).isEqualTo(1);
    }
    assertThat(getPackageMeasure("functions").getIntValue()).isEqualTo(57);
    if (Tests.is_after_plugin_1_0()) {
      assertThat(getPackageMeasure("comment_lines_density").getValue()).isEqualTo(13.9);
      assertThat(getPackageMeasure("comment_lines").getIntValue()).isEqualTo(87);
    } else {
      assertThat(getPackageMeasure("comment_lines_density").getValue()).isEqualTo(17.8);
      assertThat(getPackageMeasure("comment_lines").getIntValue()).isEqualTo(117);
    }

    assertThat(getPackageMeasure("duplicated_lines").getIntValue()).isEqualTo(0);
    assertThat(getPackageMeasure("duplicated_blocks").getIntValue()).isEqualTo(0);
    assertThat(getPackageMeasure("duplicated_lines_density").getValue()).isEqualTo(0.0);
    assertThat(getPackageMeasure("duplicated_files").getIntValue()).isEqualTo(0);

    assertThat(getPackageMeasure("violations").getIntValue()).isEqualTo(4);

    assertThat(getPackageMeasure("complexity").getIntValue()).isEqualTo(100);
    assertThat(getPackageMeasure("function_complexity").getValue()).isEqualTo(1.8);
    assertThat(getPackageMeasure("class_complexity").getValue()).isEqualTo(16.7);
    assertThat(getPackageMeasure("file_complexity_distribution").getData()).isEqualTo("0=1;5=1;10=2;20=1;30=1;60=0;90=0");
    assertThat(getPackageMeasure("class_complexity_distribution")).isNull();
    assertThat(getPackageMeasure("function_complexity_distribution").getData()).isEqualTo("1=31;2=20;4=6;6=0;8=0;10=0;12=0");
  }

  @Test
  public void testPackageCoverage() {
    Assume.assumeTrue(Tests.is_after_plugin_1_0());

    // We are getting different results for different Java versions : 1.6.0_21 and 1.5.0_16
    assertThat(getPackageMeasure("coverage").getValue()).isEqualTo(88.3, Delta.delta(0.3));
    assertThat(getPackageMeasure("line_coverage").getValue()).isEqualTo(99.6, Delta.delta(0.2));
    assertThat(getPackageMeasure("lines_to_cover").getValue()).isEqualTo(278.0, Delta.delta(2.0));
    assertThat(getPackageMeasure("uncovered_lines").getValue()).isIn(1.0, 2.0);

    assertThat(getPackageMeasure("tests")).isNull();
    assertThat(getPackageMeasure("test_success_density")).isNull();
  }

  /*
   * ====================== FILE LEVEL ======================
   */

  @Test
  public void filesMetrics() {
    assertThat(getFileMeasure("ncloc").getIntValue()).isEqualTo(238);
    assertThat(getFileMeasure("lines").getIntValue()).isEqualTo(319);
    assertThat(getFileMeasure("files").getIntValue()).isEqualTo(1);
    assertThat(getFileMeasure("classes").getIntValue()).isEqualTo(1);
    assertThat(getFileMeasure("packages")).isNull();
    assertThat(getFileMeasure("functions").getIntValue()).isEqualTo(18);
    if (Tests.is_after_plugin_1_0()) {
      assertThat(getFileMeasure("comment_lines_density").getValue()).isEqualTo(13.1);
      assertThat(getFileMeasure("comment_lines").getIntValue()).isEqualTo(36);
    } else {
      assertThat(getFileMeasure("comment_lines_density").getValue()).isEqualTo(12.8);
      assertThat(getFileMeasure("comment_lines").getIntValue()).isEqualTo(35);
    }

    assertThat(getFileMeasure("duplicated_lines")).isNull();
    assertThat(getFileMeasure("duplicated_blocks")).isNull();
    assertThat(getFileMeasure("duplicated_files")).isNull();
    assertThat(getFileMeasure("duplicated_lines_density")).isNull();

    assertThat(getFileMeasure("violations").getIntValue()).isEqualTo(4);

    assertThat(getFileMeasure("complexity").getIntValue()).isEqualTo(36);
    assertThat(getFileMeasure("function_complexity").getValue()).isEqualTo(2.0);
    assertThat(getFileMeasure("class_complexity").getValue()).isEqualTo(36.0);
    assertThat(getFileMeasure("file_complexity_distribution")).isNull();
    assertThat(getFileMeasure("class_complexity_distribution")).isNull();
    assertThat(getFileMeasure("function_complexity_distribution")).isNull();
  }

  @Test
  public void testFileCoverage() {
    Assume.assumeTrue(Tests.is_after_plugin_1_0());

    // We are getting different results for different Java versions : 1.6.0_21 and 1.5.0_16
    assertThat(getFileMeasure("coverage").getValue()).isEqualTo(86.5, Delta.delta(0.2));
    assertThat(getFileMeasure("line_coverage").getValue()).isEqualTo(100.0);
    assertThat(getFileMeasure("lines_to_cover").getValue()).isEqualTo(135.0, Delta.delta(1.0));
    assertThat(getFileMeasure("uncovered_lines").getValue()).isEqualTo(0.0);

    assertThat(getFileMeasure("tests")).isNull();
    assertThat(getFileMeasure("test_success_density")).isNull();
  }

  @Test
  public void ncloc_data_should_be_saved() {
    Assume.assumeTrue(Tests.is_after_plugin_1_0());

    String nclocData = getFileMeasure("ncloc_data").getData();
    assertThat(nclocData).contains("16=1");
    assertThat(nclocData).contains("35=1");
  }

  private Measure getFileMeasure(String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(FILE_REPORT_WRITER, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  private Measure getPackageMeasure(String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PACKAGE_REPORT, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  private Measure getProjectMeasure(String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_CODENARC, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

}
