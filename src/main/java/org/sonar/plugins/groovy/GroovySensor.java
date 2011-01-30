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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RuleFinder;
import org.sonar.plugins.groovy.codenarc.CodeNarcProfileExporter;
import org.sonar.plugins.groovy.codenarc.CodeNarcRunner;
import org.sonar.plugins.groovy.codenarc.CodeNarcXMLParser;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFile;
import org.sonar.plugins.groovy.foundation.GroovyPackage;
import org.sonar.plugins.groovy.foundation.GroovyRecognizer;
import org.sonar.plugins.groovy.gmetrics.GMetricsRunner;
import org.sonar.plugins.groovy.gmetrics.GMetricsXMLParser;
import org.sonar.plugins.groovy.utils.GroovyUtils;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.text.Source;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroovySensor implements Sensor {

  private Groovy groovy;
  private RulesProfile rulesProfile;
  private CodeNarcProfileExporter profileExporter;
  private Configuration configuration;
  private RuleFinder ruleFinder;

  public GroovySensor(Groovy groovy, RulesProfile rulesProfile, CodeNarcProfileExporter profileExporter,
      Configuration configuration, RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
    this.groovy = groovy;
    this.rulesProfile = rulesProfile;
    this.profileExporter = profileExporter;
    this.configuration = configuration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.getLanguage().equals(Groovy.INSTANCE);
  }

  public void analyse(Project project, SensorContext context) {
    computeBaseMetrics(context, project);
    computeGMetricsReport(project, context);
    computeCodeNarcReport(project, context);
  }

  private void computeGMetricsReport(Project project, SensorContext context) {
    // Should we reuse existing report from GMetrics ?
    if (StringUtils.isNotBlank(configuration.getString(GroovyPlugin.GMETRICS_REPORT_PATH))) {
      // Yes
      File gmetricsReport = getReport(project, GroovyPlugin.GMETRICS_REPORT_PATH);
      if (gmetricsReport != null) {
        new GMetricsXMLParser().parseAndProcessGMetricsResults(gmetricsReport, context);
      }
    } else {
      // No, run GMetrics
      List<File> listDirs = project.getFileSystem().getSourceDirs();
      for (File sourceDir : listDirs) {
        new GMetricsRunner().execute(sourceDir, project);
        File report = new File(project.getFileSystem().getSonarWorkingDirectory(), "gmetrics-report.xml");
        new GMetricsXMLParser().parseAndProcessGMetricsResults(report, context);
      }
    }
  }

  private void computeCodeNarcReport(Project project, SensorContext context) {
    // Should we reuse existing report from CodeNarc ?
    if (StringUtils.isNotBlank(configuration.getString(GroovyPlugin.CODENARC_REPORT_PATH))) {
      // Yes
      File codeNarcReport = getReport(project, GroovyPlugin.CODENARC_REPORT_PATH);
      if (codeNarcReport != null) {
        new CodeNarcXMLParser(context, ruleFinder).parseAndLogCodeNarcResults(codeNarcReport);
      }
    } else {
      // No, run CodeNarc
      List<File> listDirs = project.getFileSystem().getSourceDirs();
      for (File sourceDir : listDirs) {
        // TODO use container injection
        new CodeNarcRunner(rulesProfile, profileExporter, project).execute(sourceDir);
        File report = new File(project.getFileSystem().getSonarWorkingDirectory(), "codenarc-report.xml");
        new CodeNarcXMLParser(context, ruleFinder).parseAndLogCodeNarcResults(report);
      }
    }
  }

  protected File getReport(Project project, String reportProperty) {
    File report = getReportFromProperty(project, reportProperty);
    if (report == null || !report.exists() || !report.isFile()) {
      GroovyUtils.LOG.warn("Groovy report " + reportProperty + " not found at {}", report);
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
    Set<Resource> packageList = new HashSet();
    for (File groovyFile : fileSystem.getSourceFiles(groovy)) {
      try {
        reader = new StringReader(FileUtils.readFileToString(groovyFile, fileSystem.getSourceCharset().name()));
        Resource resource = GroovyFile.fromIOFile(groovyFile, fileSystem.getSourceDirs());
        Source source = new Source(reader, new GroovyRecognizer());
        packageList.add(new GroovyPackage(resource.getParent().getKey()));
        sensorContext.saveMeasure(resource, CoreMetrics.LINES, (double) source.getMeasure(Metric.LINES));
        sensorContext.saveMeasure(resource, CoreMetrics.NCLOC, (double) source.getMeasure(Metric.LINES_OF_CODE));
        sensorContext.saveMeasure(resource, CoreMetrics.COMMENT_LINES, (double) source.getMeasure(Metric.COMMENT_LINES));
        sensorContext.saveMeasure(resource, CoreMetrics.FILES, 1.0);
        sensorContext.saveMeasure(resource, CoreMetrics.CLASSES, 1.0);
      } catch (Exception e) {
        GroovyUtils.LOG.error("Can not analyze the file " + groovyFile.getAbsolutePath(), e);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
    for (Resource pack : packageList) {
      sensorContext.saveMeasure(pack, CoreMetrics.PACKAGES, 1.0);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
