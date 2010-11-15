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
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.api.checks.templates.CheckTemplateRepository;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.groovy.codenarc.CodeNarcCheckTemplateRepository;
import org.sonar.plugins.groovy.codenarc.CodeNarcRunner;
import org.sonar.plugins.groovy.codenarc.CodeNarcXMLParser;
import org.sonar.plugins.groovy.codenarc.GroovyMessageDispatcher;
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
import java.util.*;

public class GroovySensor implements Sensor {

  private Groovy groovy;
  private CheckProfile checkProfile;
  private Configuration configuration;
  private CheckTemplateRepository repo;

  public GroovySensor(Groovy groovy, CheckProfile checkProfile, Configuration configuration, CodeNarcCheckTemplateRepository repo) {
    this.groovy = groovy;
    this.checkProfile = checkProfile;
    this.configuration = configuration;
    this.repo = repo;
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
    String gmetricsReportPath = configuration.getString(GroovyPlugin.GMETRICS_REPORT_PATH);
    if (StringUtils.isNotBlank(gmetricsReportPath)) {
      // Yes
      File gmetricsReport = getReport(project, gmetricsReportPath);
      if (gmetricsReport != null) {
        GMetricsXMLParser xmlParser = new GMetricsXMLParser();
        xmlParser.parseAndProcessGMetricsResults(gmetricsReport, context);
      }
    }
    // No, run Gmetrics
    else {
      List<File> listDirs = project.getFileSystem().getSourceDirs();
      for (File sourceDir : listDirs) {
        new GMetricsRunner().execute(sourceDir, project);
        File report = new File(project.getFileSystem().getSonarWorkingDirectory(), "gmetrics-report.xml");
        GMetricsXMLParser gMetricsXMLParser = new GMetricsXMLParser();
        gMetricsXMLParser.parseAndProcessGMetricsResults(report, context);

      }
    }
  }

  private void computeCodeNarcReport(Project project, SensorContext context) {

    // Should we reuse existing report from CodeNarc ?
    String codeNarcReportPath = configuration.getString(GroovyPlugin.CODENARC_REPORT_PATH);
    GroovyMessageDispatcher messageDispatcher = new GroovyMessageDispatcher(checkProfile, project, groovy, context, repo);
    if (StringUtils.isNotBlank(codeNarcReportPath)) {
      // Yes
      File codeNarcReport = getReport(project, codeNarcReportPath);
      if (codeNarcReport != null) {
        CodeNarcXMLParser codeNarcXMLParser = new CodeNarcXMLParser(messageDispatcher);
        codeNarcXMLParser.parseAndLogCodeNarcResults(codeNarcReport);
      }
    }
    // No, run CodeNarc
    else {
      List<File> listDirs = project.getFileSystem().getSourceDirs();
      for (File sourceDir : listDirs) {
        new CodeNarcRunner().execute(sourceDir, checkProfile, project);
        File report = new File(project.getFileSystem().getSonarWorkingDirectory(), "codenarc-report.xml");
        CodeNarcXMLParser codeNarcXMLParser = new CodeNarcXMLParser(messageDispatcher);
        codeNarcXMLParser.parseAndLogCodeNarcResults(report);

      }
    }
  }

  protected File getReport(Project project, String reportPath) {
    File report = getReportFromProperty(project, reportPath);
    if (report == null || !report.exists() || !report.isFile()) {
      GroovyUtils.LOG.warn("Groovy report : " + reportPath + " not found at {}", report);
      report = null;
    }
    return report;
  }

  private File getReportFromProperty(Project project, String reportPath) {
    String path = (String) project.getProperty(reportPath);
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
      }
      catch (Exception e) {
        GroovyUtils.LOG.error("Can not analyze the file " + groovyFile.getAbsolutePath(), e);
      } finally {
        IOUtils.closeQuietly(reader);
      }

    }
    for(Resource pack : packageList) {
      sensorContext.saveMeasure(pack, CoreMetrics.PACKAGES, 1.0);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
