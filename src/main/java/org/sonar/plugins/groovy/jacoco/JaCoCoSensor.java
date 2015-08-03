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

import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.util.Collection;

public class JaCoCoSensor implements Sensor {

  private final JaCoCoConfiguration configuration;
  private final ModuleFileSystem moduleFileSystem;
  private final FileSystem fileSystem;
  private final PathResolver pathResolver;

  public JaCoCoSensor(JaCoCoConfiguration configuration, ModuleFileSystem moduleFileSystem, FileSystem fileSystem, PathResolver pathResolver) {
    this.configuration = configuration;
    this.moduleFileSystem = moduleFileSystem;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
  }

  @DependsUpon
  public String dependsUponSurefireSensors() {
    return "surefire-java";
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    new UnitTestsAnalyzer().analyse(project, context);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    File report = pathResolver.relativeFile(fileSystem.baseDir(), configuration.getReportPath());
    boolean foundReport = report.exists() && report.isFile();
    boolean shouldExecute = configuration.shouldExecuteOnProject(foundReport);
    if (!foundReport && shouldExecute) {
      JaCoCoExtensions.logger().info("JaCoCoSensor: JaCoCo report not found.");
    }
    return shouldExecute;
  }

  class UnitTestsAnalyzer extends AbstractAnalyzer {
    public UnitTestsAnalyzer() {
      super(moduleFileSystem, fileSystem, pathResolver);
    }

    @Override
    protected String getReportPath(Project project) {
      return configuration.getReportPath();
    }

    @Override
    protected void saveMeasures(SensorContext context, InputFile inputFile, Collection<Measure> measures) {
      for (Measure measure : measures) {
        context.saveMeasure(inputFile, measure);
      }
    }
  }

  protected static Measure getMeasureBasedOnValue(Metric metric, Measure measure) {
    return new Measure(metric, measure.getValue());
  }

  protected static Measure getMeasureBasedOnData(Metric metric, Measure measure) {
    String data = measure.getData();
    if (data != null) {
      return new Measure(metric, data);
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
