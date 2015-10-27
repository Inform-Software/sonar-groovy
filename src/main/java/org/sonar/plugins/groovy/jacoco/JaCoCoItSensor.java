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

public class JaCoCoItSensor implements Sensor {
  private final JaCoCoConfiguration configuration;
  private final FileSystem fileSystem;
  private final PathResolver pathResolver;
  private final Groovy groovy;

  public JaCoCoItSensor(Groovy groovy, JaCoCoConfiguration configuration, FileSystem fileSystem, PathResolver pathResolver) {
    this.configuration = configuration;
    this.groovy = groovy;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    File report = pathResolver.relativeFile(fileSystem.baseDir(), configuration.getItReportPath());
    boolean foundReport = report.exists() && report.isFile();
    boolean shouldExecute = configuration.shouldExecuteOnProject(foundReport);
    if (!foundReport && shouldExecute) {
      JaCoCoExtensions.logger().info("JaCoCoItSensor: JaCoCo IT report not found.");
    }
    return shouldExecute;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    new ITAnalyzer().analyse(project, context);
  }

  class ITAnalyzer extends AbstractAnalyzer {
    public ITAnalyzer() {
      super(groovy, fileSystem, pathResolver);
    }

    @Override
    protected String getReportPath(Project project) {
      return configuration.getItReportPath();
    }

    @Override
    protected void saveMeasures(SensorContext context, InputFile inputFile, Collection<Measure> measures) {
      for (Measure measure : measures) {
        Measure itMeasure = convertForIT(measure);
        if (itMeasure != null) {
          context.saveMeasure(inputFile, itMeasure);
        }
      }
    }

    private Measure convertForIT(Measure measure) {
      Measure itMeasure = null;
      if (CoreMetrics.LINES_TO_COVER.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.IT_LINES_TO_COVER, measure);
      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.IT_UNCOVERED_LINES, measure);
      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnData(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, measure);
      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.IT_CONDITIONS_TO_COVER, measure);
      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnValue(CoreMetrics.IT_UNCOVERED_CONDITIONS, measure);
      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnData(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, measure);
      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = JaCoCoSensor.getMeasureBasedOnData(CoreMetrics.IT_CONDITIONS_BY_LINE, measure);
      }
      return itMeasure;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
