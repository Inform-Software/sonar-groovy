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
package org.sonar.plugins.groovy;

import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.plugins.groovy.cobertura.CoberturaSensor;
import org.sonar.plugins.groovy.codenarc.CodeNarcRulesDefinition;
import org.sonar.plugins.groovy.codenarc.CodeNarcSensor;
import org.sonar.plugins.groovy.codenarc.SonarWayProfile;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;
import org.sonar.plugins.groovy.jacoco.JaCoCoExtensions;
import org.sonar.plugins.groovy.surefire.GroovySurefireParser;
import org.sonar.plugins.groovy.surefire.GroovySurefireSensor;

@Properties({
  @Property(
    key = GroovyPlugin.CODENARC_REPORT_PATHS,
    name = "CodeNarc Reports",
    description = "Path to the CodeNarc XML reports. Paths may be absolute or relative to the project base directory.",
    project = true,
    module = true,
    global = true,
    deprecatedKey = GroovyPlugin.CODENARC_REPORT_PATH),
  @Property(
    key = GroovyPlugin.COBERTURA_REPORT_PATH,
    name = "Cobertura Report",
    description = "Path to the Cobertura XML report. Path may be absolute or relative to the project base directory.",
    project = true,
    module = true,
    global = true),
  @Property(
    key = GroovyPlugin.IGNORE_HEADER_COMMENTS,
    defaultValue = "true",
    name = "Ignore Header Comments",
    description = "If set to \"true\", the file headers (that are usually the same on each file: licensing information for example) are not considered as comments. " +
      "Thus metrics such as \"Comment lines\" do not get incremented. " +
      "If set to \"false\", those file headers are considered as comments and metrics such as \"Comment lines\" get incremented.",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN),
  @Property(
    key = GroovyPlugin.FILE_SUFFIXES_KEY,
    defaultValue = GroovyPlugin.DEFAULT_FILE_SUFFIXES,
    name = "File suffixes",
    description = "Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.",
    project = true,
    module = true,
    global = true),
  @Property(
    key = GroovyPlugin.SONAR_GROOVY_BINARIES,
    name = "Binary directories",
    description = "Comma-separated list of optional directories that contain the compiled groovy sources.",
    project = true,
    module = true,
    global = true)
})
public class GroovyPlugin implements Plugin {

  @Deprecated public static final String CODENARC_REPORT_PATH = "sonar.groovy.codenarc.reportPath";
  public static final String CODENARC_REPORT_PATHS = "sonar.groovy.codenarc.reportPaths";

  public static final String COBERTURA_REPORT_PATH = "sonar.groovy.cobertura.reportPath";
  public static final String IGNORE_HEADER_COMMENTS = "sonar.groovy.ignoreHeaderComments";

  public static final String SONAR_GROOVY_BINARIES = "sonar.groovy.binaries";
  public static final String SONAR_GROOVY_BINARIES_FALLBACK = "sonar.binaries";

  public static final String FILE_SUFFIXES_KEY = "sonar.groovy.file.suffixes";
  public static final String DEFAULT_FILE_SUFFIXES = ".groovy";

  @Override
  public void define(Context context) {
    context.addExtensions(
      // CodeNarc
      CodeNarcRulesDefinition.class,
      CodeNarcSensor.class,
      SonarWayProfile.class,
      // Foundation
      Groovy.class,
      GroovyFileSystem.class,
      // Main sensor
      GroovySensor.class,
      GroovyMetrics.class,
      // Surefire
      GroovySurefireParser.class,
      GroovySurefireSensor.class,
      // Cobertura
      CoberturaSensor.class);
    context.addExtensions(JaCoCoExtensions.getExtensions());
  }
}
