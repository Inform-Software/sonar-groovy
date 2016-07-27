/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.groovy.jacoco;

import com.google.common.annotations.VisibleForTesting;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;

public class JaCoCoSensor implements Sensor {

  private final JaCoCoConfiguration configuration;
  private final FileSystem fileSystem;
  private final PathResolver pathResolver;
  private final Groovy groovy;

  public JaCoCoSensor(Groovy groovy, JaCoCoConfiguration configuration, FileSystem fileSystem, PathResolver pathResolver) {
    this.configuration = configuration;
    this.groovy = groovy;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Groovy.KEY).name(this.toString());
  }

  @Override
  public void execute(SensorContext context) {
    if (shouldExecuteOnProject()) {
      new UnitTestsAnalyzer().analyse(context);
    }
  }

  @VisibleForTesting
  boolean shouldExecuteOnProject() {
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
      super(groovy, fileSystem, pathResolver);
    }

    @Override
    protected String getReportPath() {
      return configuration.getReportPath();
    }

    @Override
    protected CoverageType coverageType() {
      return CoverageType.UNIT;
    }
  }

  @Override
  public String toString() {
    return "Groovy " + getClass().getSimpleName();
  }

}
