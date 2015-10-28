/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.groovy.jacoco;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;
import java.util.Collection;

public class JaCoCoOverallSensor implements Sensor {

  public static final String JACOCO_OVERALL = "jacoco-overall.exec";

  private final JaCoCoConfiguration configuration;
  private final FileSystem fileSystem;
  private final PathResolver pathResolver;
  private final Groovy groovy;

  public JaCoCoOverallSensor(Groovy groovy, JaCoCoConfiguration configuration, FileSystem fileSystem, PathResolver pathResolver) {
    this.configuration = configuration;
    this.groovy = groovy;
    this.pathResolver = pathResolver;
    this.fileSystem = fileSystem;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    File baseDir = fileSystem.baseDir();
    File reportUTs = pathResolver.relativeFile(baseDir, configuration.getReportPath());
    File reportITs = pathResolver.relativeFile(baseDir, configuration.getItReportPath());
    boolean foundOneReport = reportUTs.exists() || reportITs.exists();
    boolean shouldExecute = configuration.shouldExecuteOnProject(foundOneReport);
    if (!foundOneReport && shouldExecute) {
      JaCoCoExtensions.logger().info("JaCoCoOverallSensor: JaCoCo reports not found.");
    }
    return shouldExecute;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    File baseDir = fileSystem.baseDir();
    File reportUTs = pathResolver.relativeFile(baseDir, configuration.getReportPath());
    File reportITs = pathResolver.relativeFile(baseDir, configuration.getItReportPath());

    File reportOverall = new File(fileSystem.workDir(), JACOCO_OVERALL);
    reportOverall.getParentFile().mkdirs();

    JaCoCoReportMerger.mergeReports(reportOverall, reportUTs, reportITs);

    new OverallAnalyzer(reportOverall).analyse(project, context);
  }

  class OverallAnalyzer extends AbstractAnalyzer {
    private final File report;

    OverallAnalyzer(File report) {
      super(groovy, fileSystem, pathResolver);
      this.report = report;
    }

    @Override
    protected String getReportPath(Project project) {
      return report.getAbsolutePath();
    }

    @Override
    protected void saveMeasures(SensorContext context, InputFile inputFile, Collection<Measure> measures) {
      for (Measure measure : measures) {
        Measure mergedMeasure = convertForOverall(measure);
        if (mergedMeasure != null) {
          context.saveMeasure(inputFile, mergedMeasure);
        }
      }
    }

    private Measure convertForOverall(Measure measure) {
      Measure itMeasure = null;
      if (CoreMetrics.LINES_TO_COVER.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.OVERALL_LINES_TO_COVER, measure);
      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.OVERALL_UNCOVERED_LINES, measure);
      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnData(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, measure);
      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, measure);
      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, measure);
      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnData(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, measure);
      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnData(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, measure);
      }
      return itMeasure;
    }
  }

  @Override
  public String toString() {
    return "Groovy " + getClass().getSimpleName();
  }
}
