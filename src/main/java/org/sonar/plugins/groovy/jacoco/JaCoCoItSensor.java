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
package org.sonar.plugins.groovy.jacoco;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.util.Collection;

public class JaCoCoItSensor implements Sensor {
  private final JaCoCoConfiguration configuration;
  private final ModuleFileSystem moduleFileSystem;
  private final FileSystem fileSystem;
  private final PathResolver pathResolver;

  public JaCoCoItSensor(JaCoCoConfiguration configuration, ModuleFileSystem moduleFileSystem, FileSystem fileSystem, PathResolver pathResolver) {
    this.configuration = configuration;
    this.moduleFileSystem = moduleFileSystem;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    File report = pathResolver.relativeFile(fileSystem.baseDir(), configuration.getItReportPath());
    boolean foundReport = report.exists() && report.isFile();
    boolean shouldExecute = configuration.shouldExecuteOnProject(foundReport);
    if (!foundReport && shouldExecute) {
      JaCoCoExtensions.LOG.info("JaCoCoItSensor: JaCoCo IT report not found.");
    }
    return shouldExecute;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    new ITAnalyzer().analyse(project, context);
  }

  class ITAnalyzer extends AbstractAnalyzer {
    public ITAnalyzer() {
      super(moduleFileSystem, fileSystem, pathResolver);
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
        itMeasure = new Measure(CoreMetrics.IT_LINES_TO_COVER, measure.getValue());

      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_UNCOVERED_LINES, measure.getValue());

      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, measure.getData());

      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_CONDITIONS_TO_COVER, measure.getValue());

      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_UNCOVERED_CONDITIONS, measure.getValue());

      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, measure.getData());

      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_CONDITIONS_BY_LINE, measure.getData());
      }
      return itMeasure;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
