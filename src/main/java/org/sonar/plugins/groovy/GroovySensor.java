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
import org.gmetrics.GMetricsRunner;
import org.gmetrics.metricset.DefaultMetricSet;
import org.gmetrics.result.MetricResult;
import org.gmetrics.result.NumberMetricResult;
import org.gmetrics.result.SingleNumberMetricResult;
import org.gmetrics.resultsnode.ClassResultsNode;
import org.gmetrics.resultsnode.ResultsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyRecognizer;
import org.sonar.plugins.groovy.gmetrics.CustomSourceAnalyzer;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.text.Source;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class GroovySensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GroovySensor.class);

  private static final Number[] FUNCTIONS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12};
  private static final Number[] FILES_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};

  private final Groovy groovy;

  public GroovySensor(Groovy groovy) {
    this.groovy = groovy;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Groovy.KEY.equals(project.getLanguageKey());
  }

  public void analyse(Project project, SensorContext context) {
    computeBaseMetrics(context, project);
    computeGMetricsReport(project, context);
  }

  private void computeGMetricsReport(Project project, SensorContext context) {
    for (File sourceDir : project.getFileSystem().getSourceDirs()) {
      processDirectory(context, project, sourceDir);
    }
  }

  private void processDirectory(SensorContext context, Project project, File sourceDir) {
    GMetricsRunner runner = new GMetricsRunner();
    runner.setMetricSet(new DefaultMetricSet());
    CustomSourceAnalyzer analyzer = new CustomSourceAnalyzer(sourceDir.getAbsolutePath());
    runner.setSourceAnalyzer(analyzer);
    runner.execute();

    for (Entry<File, Collection<ClassResultsNode>> entry : analyzer.getResultsByFile().asMap().entrySet()) {
      File file = entry.getKey();
      Collection<ClassResultsNode> results = entry.getValue();
      org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(file, project.getFileSystem().getSourceDirs());
      processFile(context, sonarFile, results);
    }
  }

  private void processFile(SensorContext context, org.sonar.api.resources.File sonarFile, Collection<ClassResultsNode> results) {
    double methods = 0;
    double complexity = 0;

    RangeDistributionBuilder functionsComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, FUNCTIONS_DISTRIB_BOTTOM_LIMITS);

    for (ClassResultsNode result : results) {
      for (ResultsNode resultsNode : result.getChildren().values()) {
        methods += 1;
        for (MetricResult metricResult : resultsNode.getMetricResults()) {
          String metricName = metricResult.getMetric().getName();
          if ("CyclomaticComplexity".equals(metricName)) {
            int value = (Integer) ((SingleNumberMetricResult) metricResult).getNumber();
            functionsComplexityDistribution.add(value);
          }
        }
      }

      for (MetricResult metricResult : result.getMetricResults()) {
        String metricName = metricResult.getMetric().getName();
        if ("CyclomaticComplexity".equals(metricName)) {
          int value = (Integer) ((NumberMetricResult) metricResult).getValues().get("total");
          complexity += value;
        }
      }
    }

    context.saveMeasure(sonarFile, CoreMetrics.FUNCTIONS, methods);
    context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, complexity);

    context.saveMeasure(sonarFile, functionsComplexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
    RangeDistributionBuilder fileComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, FILES_DISTRIB_BOTTOM_LIMITS);
    fileComplexityDistribution.add(complexity);
    context.saveMeasure(sonarFile, fileComplexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
  }

  public static File getReport(Project project, String reportProperty) {
    File report = getReportFromProperty(project, reportProperty);
    if (report == null || !report.exists() || !report.isFile()) {
      LOG.warn("Groovy report " + reportProperty + " not found at {}", report);
      report = null;
    }
    return report;
  }

  private static File getReportFromProperty(Project project, String reportProperty) {
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
