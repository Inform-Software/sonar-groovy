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

import java.util.Arrays;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.groovy.foundation.Groovy;

@ScannerSide
public class JaCoCoConfiguration {

  public static final String REPORT_PATH_PROPERTY = "sonar.groovy.jacoco.reportPath";
  public static final String REPORT_PATH_DEFAULT_VALUE = "target/jacoco.exec";
  public static final String IT_REPORT_PATH_PROPERTY = "sonar.groovy.jacoco.itReportPath";
  public static final String IT_REPORT_PATH_DEFAULT_VALUE = "target/jacoco-it.exec";
  public static final String REPORT_MISSING_FORCE_ZERO = "sonar.groovy.jacoco.reportMissing.force.zero";
  public static final boolean REPORT_MISSING_FORCE_ZERO_DEFAULT_VALUE = false;

  private final Settings settings;
  private final FileSystem fileSystem;

  public JaCoCoConfiguration(Settings settings, FileSystem fileSystem) {
    this.settings = settings;
    this.fileSystem = fileSystem;
  }

  public boolean shouldExecuteOnProject(boolean reportFound) {
    return hasGroovyFiles() && (reportFound || isCoverageToZeroWhenNoReport());
  }

  private boolean hasGroovyFiles() {
    return fileSystem.hasFiles(fileSystem.predicates().hasLanguage(Groovy.KEY));
  }

  public String getReportPath() {
    return settings.getString(REPORT_PATH_PROPERTY);
  }

  public String getItReportPath() {
    return settings.getString(IT_REPORT_PATH_PROPERTY);
  }

  private boolean isCoverageToZeroWhenNoReport() {
    return settings.getBoolean(REPORT_MISSING_FORCE_ZERO);
  }

  public static List<PropertyDefinition> getPropertyDefinitions() {
    return Arrays.asList(
      PropertyDefinition.builder(JaCoCoConfiguration.REPORT_PATH_PROPERTY)
        .defaultValue(JaCoCoConfiguration.REPORT_PATH_DEFAULT_VALUE)
        .name("UT JaCoCo Report")
        .description("Path to the JaCoCo report file containing coverage data by unit tests. The path may be absolute or relative to the project base directory.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),
      PropertyDefinition.builder(JaCoCoConfiguration.IT_REPORT_PATH_PROPERTY)
        .defaultValue(JaCoCoConfiguration.IT_REPORT_PATH_DEFAULT_VALUE)
        .name("IT JaCoCo Report")
        .description("Path to the JaCoCo report file containing coverage data by integration tests. The path may be absolute or relative to the project base directory.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),
      PropertyDefinition.builder(JaCoCoConfiguration.REPORT_MISSING_FORCE_ZERO)
        .defaultValue(Boolean.toString(JaCoCoConfiguration.REPORT_MISSING_FORCE_ZERO_DEFAULT_VALUE))
        .name("Force zero coverage")
        .description("Force coverage to 0% if no JaCoCo reports are found during analysis.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .type(PropertyType.BOOLEAN)
        .build());
  }
}
