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

package org.sonar.plugins.groovy;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.ResourceQuery;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.core.Is.is;

public class GroovyIT {
  private static Sonar sonar;
  private static final String PROJECT_CODENARC = "org.codenarc:CodeNarc";
  private static final String PACKAGE_REPORT = "org.codenarc:CodeNarc:org.codenarc.report";
  private static final String FILE_REPORT_WRITER = "org.codenarc:CodeNarc:org.codenarc.report.HtmlReportWriter";

  @BeforeClass
  public static void buildServer() {
    sonar = Sonar.create("http://localhost:9000");
  }

  @Test
  public void codenarcIsAnalyzed() {
    assertThat(sonar.find(new ResourceQuery(PROJECT_CODENARC)).getName(), is("CodeNarc"));
    assertThat(sonar.find(new ResourceQuery(PROJECT_CODENARC)).getVersion(), is("0.9"));
    assertThat(sonar.find(new ResourceQuery(PACKAGE_REPORT)).getName(), is("org.codenarc.report"));
  }

  @Test
  public void projectsMetrics() {
    assertThat(getProjectMeasure("ncloc").getIntValue(), is(4199));
    assertThat(getProjectMeasure("lines").getIntValue(), is(9439));
    assertThat(getProjectMeasure("files").getIntValue(), is(198));
    assertThat(getProjectMeasure("classes").getIntValue(), is(135));
    assertThat(getProjectMeasure("packages").getIntValue(), is(22));
    assertThat(getProjectMeasure("functions").getIntValue(), is(365));
    assertThat(getProjectMeasure("comment_lines_density").getValue(), is(40.5));
    assertThat(getProjectMeasure("comment_lines").getIntValue(), is(2856));

    assertThat(getProjectMeasure("duplicated_lines").getIntValue(), is(54));
    assertThat(getProjectMeasure("duplicated_blocks").getIntValue(), is(2));
    assertThat(getProjectMeasure("duplicated_lines_density").getValue(), is(0.6));
    assertThat(getProjectMeasure("duplicated_files").getIntValue(), is(2));

    assertThat(getProjectMeasure("complexity").getIntValue(), is(816));
    assertThat(getProjectMeasure("function_complexity").getValue(), is(2.2));
    assertThat(getProjectMeasure("class_complexity").getValue(), is(6.0));
    assertThat(getProjectMeasure("violations").getIntValue(), is(12));
    assertThat(getProjectMeasure("violations_density").getValue(), is(99.3));
    assertThat(getProjectMeasure("class_complexity_distribution").getData(), is("0=138;5=31;10=19;20=7;30=1;60=0;90=0"));
    assertThat(getProjectMeasure("function_complexity_distribution").getData(), is("1=177;2=133;4=38;6=11;8=1;10=4;12=1"));

    // We are getting different results for different Java versions : 1.6.0_21 and 1.5.0_16
    assertThat("coverage", getProjectMeasure("coverage").getValue(), anyOf(
      is(89.8),
      is(90.0),
      is(89.9) // java 1.6.0_20
      ));
    assertThat(getProjectMeasure("line_coverage").getValue(), anyOf(is(98.9), is(98.8)));
    assertThat(getProjectMeasure("lines_to_cover").getValue(), anyOf(is(1802.0), is(1806.0), is(1805.0)));
    assertThat(getProjectMeasure("uncovered_lines").getValue(), anyOf(is(20.0), is(21.0), is(19.0)));

    assertThat(getProjectMeasure("tests").getValue(), is(1201.0));
    assertThat(getProjectMeasure("test_success_density").getValue(), anyOf(is(99.8), is(99.9)));
  }

  @Test
  public void packagesMetrics() {
    assertThat(getPackageMeasure("ncloc").getIntValue(), is(540));
    assertThat(getPackageMeasure("lines").getIntValue(), is(807));
    assertThat(getPackageMeasure("files").getIntValue(), is(6));
    assertThat(getPackageMeasure("classes").getIntValue(), is(6));
    assertThat(getPackageMeasure("packages").getIntValue(), is(1));
    assertThat(getPackageMeasure("functions").getIntValue(), is(56));
    assertThat(getPackageMeasure("comment_lines_density").getValue(), is(17.8));
    assertThat(getPackageMeasure("comment_lines").getIntValue(), is(117));

    assertThat(getPackageMeasure("duplicated_lines").getIntValue(), is(0));
    assertThat(getPackageMeasure("duplicated_blocks").getIntValue(), is(0));
    assertThat(getPackageMeasure("duplicated_lines_density").getValue(), is(0.0));
    assertThat(getPackageMeasure("duplicated_files").getIntValue(), is(0));

    assertThat(getPackageMeasure("complexity").getIntValue(), is(96));
    assertThat(getPackageMeasure("function_complexity").getValue(), is(1.7));
    assertThat(getPackageMeasure("class_complexity").getValue(), is(16.0));
    assertThat(getPackageMeasure("violations").getIntValue(), is(4));
    assertThat(getPackageMeasure("violations_density").getValue(), is(97.8));
    assertThat(getPackageMeasure("class_complexity_distribution").getData(), is("0=2;5=0;10=2;20=1;30=1;60=0;90=0"));
    assertThat(getPackageMeasure("function_complexity_distribution").getData(), is("1=31;2=20;4=5;6=0;8=0;10=0;12=0"));

    // We are getting different results for different Java versions : 1.6.0_21 and 1.5.0_16
    assertThat(getPackageMeasure("coverage").getValue(), anyOf(is(88.8), is(89.1)));
    assertThat(getPackageMeasure("line_coverage").getValue(), anyOf(is(99.7), is(99.3)));
    assertThat(getPackageMeasure("lines_to_cover").getValue(), anyOf(is(304.0), is(303.0), is(305.0)));
    assertThat(getPackageMeasure("uncovered_lines").getValue(), anyOf(is(1.0), is(2.0)));

    assertThat(getPackageMeasure("tests").getValue(), is(60.0));
    assertThat(getPackageMeasure("test_success_density").getValue(), is(100.0));
  }

  @Test
  public void filesMetrics() {
    assertThat(getFileMeasure("ncloc").getIntValue(), is(238));
    assertThat(getFileMeasure("lines").getIntValue(), is(319));
    assertThat(getFileMeasure("files").getIntValue(), is(1));
    assertThat(getFileMeasure("classes").getIntValue(), is(1));
    assertNull(getFileMeasure("packages"));
    assertThat(getFileMeasure("functions").getIntValue(), is(18));
    assertThat(getFileMeasure("comment_lines_density").getValue(), is(12.8));
    assertThat(getFileMeasure("comment_lines").getIntValue(), is(35));
    assertNull(getFileMeasure("duplicated_lines"));
    assertNull(getFileMeasure("duplicated_blocks"));
    assertNull(getFileMeasure("duplicated_files"));
    assertNull(getFileMeasure("duplicated_lines_density"));
    assertThat(getFileMeasure("complexity").getIntValue(), is(36));
    assertThat(getFileMeasure("function_complexity").getValue(), is(2.0));
    assertThat(getFileMeasure("class_complexity").getValue(), is(36.0));
    assertThat(getFileMeasure("violations").getIntValue(), is(4));
    assertThat(getFileMeasure("violations_density").getValue(), is(95.0));
    assertNull(getFileMeasure("class_complexity_distribution"));
    assertNull(getFileMeasure("function_complexity_distribution"));

    // We are getting different results for different Java versions : 1.6.0_21 and 1.5.0_16
    assertThat(getFileMeasure("coverage").getValue(), anyOf(is(87.1), is(87.7)));
    assertThat(getFileMeasure("line_coverage").getValue(), is(100.0));
    assertThat(getFileMeasure("lines_to_cover").getValue(), anyOf(is(148.0), is(149.0)));
    assertThat(getFileMeasure("uncovered_lines").getValue(), is(0.0));

    assertNull(getFileMeasure("tests"));
    assertNull(getFileMeasure("test_success_density"));
  }

  private Measure getFileMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(FILE_REPORT_WRITER, metricKey)).getMeasure(metricKey);
  }

  private Measure getPackageMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(PACKAGE_REPORT, metricKey)).getMeasure(metricKey);
  }

  private Measure getProjectMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(PROJECT_CODENARC, metricKey)).getMeasure(metricKey);
  }
}
