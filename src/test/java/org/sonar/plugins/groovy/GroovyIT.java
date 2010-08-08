/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.groovy;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.ResourceQuery;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
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
  public void strutsIsAnalyzed() {
    assertThat(sonar.find(new ResourceQuery(PROJECT_CODENARC)).getName(), is("CodeNarc"));
    assertThat(sonar.find(new ResourceQuery(PROJECT_CODENARC)).getVersion(), is("0.9"));
    assertThat(sonar.find(new ResourceQuery(PACKAGE_REPORT)).getName(), is("org.codenarc.report"));
  }

  @Test
  public void projectsMetrics() {
    Assert.assertThat(getProjectMeasure("ncloc").getIntValue(), is(4199));
    Assert.assertThat(getProjectMeasure("lines").getIntValue(), is(9439));
    Assert.assertThat(getProjectMeasure("files").getIntValue(), is(198));
    Assert.assertThat(getProjectMeasure("classes").getIntValue(), is(135));
    Assert.assertThat(getProjectMeasure("packages").getIntValue(), is(22));
    Assert.assertThat(getProjectMeasure("functions").getIntValue(), is(365));
    Assert.assertThat(getProjectMeasure("comment_lines_density").getValue(), is(40.5));
    Assert.assertThat(getProjectMeasure("comment_lines").getIntValue(), is(2856));

    Assert.assertThat(getProjectMeasure("duplicated_lines").getIntValue(), is(54));
    Assert.assertThat(getProjectMeasure("duplicated_blocks").getIntValue(), is(2));
    Assert.assertThat(getProjectMeasure("duplicated_lines_density").getValue(), is(0.6));
    Assert.assertThat(getProjectMeasure("duplicated_files").getIntValue(), is(2));

    Assert.assertThat(getProjectMeasure("complexity").getIntValue(), is(816));
    Assert.assertThat(getProjectMeasure("function_complexity").getValue(), is(2.2));
    Assert.assertThat(getProjectMeasure("class_complexity").getValue(), is(6.0));
    Assert.assertThat(getProjectMeasure("violations").getIntValue(), is(12));
    Assert.assertThat(getProjectMeasure("violations_density").getValue(), is(99.3));
    Assert.assertThat(getProjectMeasure("class_complexity_distribution").getData(), is("0=138;5=31;10=19;20=7;30=1;60=0;90=0"));
    Assert.assertThat(getProjectMeasure("function_complexity_distribution").getData(), is("1=177;2=133;4=38;6=11;8=1;10=4;12=1"));
  }

  @Test
  public void packagesMetrics() {
    Assert.assertThat(getPackageMeasure("ncloc").getIntValue(), is(540));
    Assert.assertThat(getPackageMeasure("lines").getIntValue(), is(807));
    Assert.assertThat(getPackageMeasure("files").getIntValue(), is(6));
    Assert.assertThat(getPackageMeasure("classes").getIntValue(), is(6));
    Assert.assertThat(getPackageMeasure("packages").getIntValue(), is(1));
    Assert.assertThat(getPackageMeasure("functions").getIntValue(), is(56));
    Assert.assertThat(getPackageMeasure("comment_lines_density").getValue(), is(17.8));
    Assert.assertThat(getPackageMeasure("comment_lines").getIntValue(), is(117));

    Assert.assertThat(getPackageMeasure("duplicated_lines").getIntValue(), is(0));
    Assert.assertThat(getPackageMeasure("duplicated_blocks").getIntValue(), is(0));
    Assert.assertThat(getPackageMeasure("duplicated_lines_density").getValue(), is(0.0));
    Assert.assertThat(getPackageMeasure("duplicated_files").getIntValue(), is(0));

    Assert.assertThat(getPackageMeasure("complexity").getIntValue(), is(96));
    Assert.assertThat(getPackageMeasure("function_complexity").getValue(), is(1.7));
    Assert.assertThat(getPackageMeasure("class_complexity").getValue(), is(16.0));
    Assert.assertThat(getPackageMeasure("violations").getIntValue(), is(4));
    Assert.assertThat(getPackageMeasure("violations_density").getValue(), is(97.8));
    Assert.assertThat(getPackageMeasure("class_complexity_distribution").getData(), is("0=2;5=0;10=2;20=1;30=1;60=0;90=0"));
    Assert.assertThat(getPackageMeasure("function_complexity_distribution").getData(), is("1=31;2=20;4=5;6=0;8=0;10=0;12=0"));
  }

  @Test
  public void filesMetrics() {
    Assert.assertThat(getFileMeasure("ncloc").getIntValue(), is(238));
    Assert.assertThat(getFileMeasure("lines").getIntValue(), is(319));
    Assert.assertThat(getFileMeasure("files").getIntValue(), is(1));
    Assert.assertThat(getFileMeasure("classes").getIntValue(), is(1));
    assertNull(getFileMeasure("packages"));
    Assert.assertThat(getFileMeasure("functions").getIntValue(), is(18));
    Assert.assertThat(getFileMeasure("comment_lines_density").getValue(), is(12.8));
    Assert.assertThat(getFileMeasure("comment_lines").getIntValue(), is(35));
    assertNull(getFileMeasure("duplicated_lines"));
    assertNull(getFileMeasure("duplicated_blocks"));
    assertNull(getFileMeasure("duplicated_files"));
    assertNull(getFileMeasure("duplicated_lines_density"));
    Assert.assertThat(getFileMeasure("complexity").getIntValue(), is(36));
    Assert.assertThat(getFileMeasure("function_complexity").getValue(), is(2.0));
    Assert.assertThat(getFileMeasure("class_complexity").getValue(), is(36.0));
    Assert.assertThat(getFileMeasure("violations").getIntValue(), is(4));
    Assert.assertThat(getFileMeasure("violations_density").getValue(), is(95.0));
    assertNull(getFileMeasure("class_complexity_distribution"));
    assertNull(getFileMeasure("function_complexity_distribution"));
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
