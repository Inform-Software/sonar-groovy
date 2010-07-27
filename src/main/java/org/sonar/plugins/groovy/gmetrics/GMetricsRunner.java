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

package org.sonar.plugins.groovy.gmetrics;

import org.gmetrics.analyzer.FilesystemSourceAnalyzer;
import org.gmetrics.metricset.DefaultMetricSet;
import org.gmetrics.report.XmlReportWriter;
import org.sonar.api.resources.Project;
import org.sonar.plugins.groovy.utils.GroovyUtils;

import java.io.File;
import java.util.Arrays;

public class GMetricsRunner {

  public void execute(File sourceDir, Project project) {
    GroovyUtils.LOG.info("Executing GMetrics");
    org.gmetrics.GMetricsRunner runner = new org.gmetrics.GMetricsRunner();
    runner.setMetricSet(new DefaultMetricSet());

    // sources
    FilesystemSourceAnalyzer sources = new FilesystemSourceAnalyzer();
    sources.setBaseDirectory(sourceDir.getAbsolutePath());
    sources.setIncludes("**/*.groovy");
    runner.setSourceAnalyzer(sources);

    // generated xml report
    XmlReportWriter report = new XmlReportWriter();
    report.setTitle("Sonar");
    report.setOutputFile(new File(project.getFileSystem().getSonarWorkingDirectory(), "gmetrics-report.xml").getAbsolutePath());
    runner.setReportWriters(Arrays.asList(report));

    runner.execute();
  }

}
