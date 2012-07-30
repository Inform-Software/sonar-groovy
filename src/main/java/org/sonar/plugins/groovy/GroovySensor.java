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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.plugins.groovy.codenarc.CodeNarcExecutor;
import org.sonar.plugins.groovy.codenarc.CodeNarcXMLParser;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyRecognizer;
import org.sonar.plugins.groovy.gmetrics.GMetricsExecutor;
import org.sonar.plugins.groovy.gmetrics.GMetricsXMLParser;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.text.Source;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroovySensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GroovySensor.class);

  private Groovy groovy;
  private GMetricsExecutor gmetricsExecutor;
  private GMetricsXMLParser gmetricsParser;
  private CodeNarcExecutor codeNarcExecutor;
  private CodeNarcXMLParser codeNarcParser;

  public GroovySensor(Groovy groovy,
      GMetricsExecutor gmetricsExecutor, GMetricsXMLParser gmetricsParser,
      CodeNarcExecutor codeNarcExecutor, CodeNarcXMLParser codeNarcParser) {
    this.gmetricsExecutor = gmetricsExecutor;
    this.gmetricsParser = gmetricsParser;
    this.codeNarcExecutor = codeNarcExecutor;
    this.codeNarcParser = codeNarcParser;
    this.groovy = groovy;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Groovy.KEY.equals(project.getLanguageKey());
  }

  public void analyse(Project project, SensorContext context) {
    computeBaseMetrics(context, project);
    computeGMetricsReport(project, context);
    computeCodeNarcReport(project, context);
  }

  private void computeGMetricsReport(Project project, SensorContext context) {
    // Should we reuse existing report from GMetrics ?
    if (StringUtils.isNotBlank((String) project.getProperty(GroovyPlugin.GMETRICS_REPORT_PATH))) {
      // Yes
      File gmetricsReport = getReport(project, GroovyPlugin.GMETRICS_REPORT_PATH);
      if (gmetricsReport != null) {
        gmetricsParser.parseAndProcessGMetricsResults(gmetricsReport, context);
      }
    } else {
      // No, run GMetrics
      List<File> listDirs = project.getFileSystem().getSourceDirs();
      for (File sourceDir : listDirs) {
        File report = gmetricsExecutor.execute(sourceDir, project);
        gmetricsParser.parseAndProcessGMetricsResults(report, context);
      }
    }
  }

  private void computeCodeNarcReport(Project project, SensorContext context) {
    // Should we reuse existing report from CodeNarc ?
    if (StringUtils.isNotBlank((String) project.getProperty(GroovyPlugin.CODENARC_REPORT_PATH))) {
      // Yes
      File codeNarcReport = getReport(project, GroovyPlugin.CODENARC_REPORT_PATH);
      if (codeNarcReport != null) {
        codeNarcParser.parseAndLogCodeNarcResults(codeNarcReport, context);
      }
    } else {
      // No, run CodeNarc
      List<File> listDirs = project.getFileSystem().getSourceDirs();
      for (File sourceDir : listDirs) {
        File report = codeNarcExecutor.execute(sourceDir);
        codeNarcParser.parseAndLogCodeNarcResults(report, context);
      }
    }
  }

  protected File getReport(Project project, String reportProperty) {
    File report = getReportFromProperty(project, reportProperty);
    if (report == null || !report.exists() || !report.isFile()) {
      LOG.warn("Groovy report " + reportProperty + " not found at {}", report);
      report = null;
    }
    return report;
  }

  private File getReportFromProperty(Project project, String reportProperty) {
    String path = (String) project.getProperty(reportProperty);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  protected void computeBaseMetrics(SensorContext sensorContext, Project project) {
    Reader reader = null;
    ProjectFileSystem fileSystem = project.getFileSystem();
    Set<org.sonar.api.resources.Directory> packageList = new HashSet<org.sonar.api.resources.Directory>();
    for (File groovyFile : fileSystem.getSourceFiles(groovy)) {
      try {
        reader = new StringReader(FileUtils.readFileToString(groovyFile, fileSystem.getSourceCharset().name()));
        org.sonar.api.resources.File resource = org.sonar.api.resources.File.fromIOFile(groovyFile, fileSystem.getSourceDirs());
        Source source = new Source(reader, new GroovyRecognizer());
        packageList.add(new org.sonar.api.resources.Directory(resource.getParent().getKey()));
        sensorContext.saveMeasure(resource, CoreMetrics.LINES, (double) source.getMeasure(Metric.LINES));
        sensorContext.saveMeasure(resource, CoreMetrics.NCLOC, (double) source.getMeasure(Metric.LINES_OF_CODE));
        sensorContext.saveMeasure(resource, CoreMetrics.COMMENT_LINES, (double) source.getMeasure(Metric.COMMENT_LINES));
        sensorContext.saveMeasure(resource, CoreMetrics.FILES, 1.0);
        // TODO file can contain more than one class
        sensorContext.saveMeasure(resource, CoreMetrics.CLASSES, 1.0);
      } catch (Exception e) {
        LOG.error("Can not analyze the file " + groovyFile.getAbsolutePath(), e);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
    for (org.sonar.api.resources.Directory pack : packageList) {
      sensorContext.saveMeasure(pack, CoreMetrics.PACKAGES, 1.0);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
