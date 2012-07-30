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

package org.sonar.plugins.groovy.gmetrics;

import org.gmetrics.GMetricsRunner;
import org.gmetrics.analyzer.FilesystemSourceAnalyzer;
import org.gmetrics.metricset.DefaultMetricSet;
import org.gmetrics.report.XmlReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Project;

import java.io.File;
import java.util.Arrays;

public class GMetricsExecutor implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(GMetricsExecutor.class);

  /**
   * @return generated XML report
   */
  public File execute(File sourceDir, Project project) {
    LOG.info("Executing GMetrics");
    GMetricsRunner runner = new GMetricsRunner();
    runner.setMetricSet(new DefaultMetricSet());

    // sources
    FilesystemSourceAnalyzer sources = new FilesystemSourceAnalyzer();
    sources.setBaseDirectory(sourceDir.getAbsolutePath());
    sources.setIncludes("**/*.groovy");
    runner.setSourceAnalyzer(sources);

    // generated XML report
    XmlReportWriter report = new XmlReportWriter();
    report.setTitle("Sonar");
    File reportFile = new File(project.getFileSystem().getSonarWorkingDirectory(), "gmetrics-report.xml");
    report.setOutputFile(reportFile.getAbsolutePath());
    runner.setReportWriters(Arrays.asList(report));

    runner.execute();

    return reportFile;
  }

}
