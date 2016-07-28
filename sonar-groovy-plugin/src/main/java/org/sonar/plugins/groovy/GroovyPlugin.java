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

import com.google.common.collect.ImmutableList;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.groovy.cobertura.CoberturaSensor;
import org.sonar.plugins.groovy.codenarc.CodeNarcRulesDefinition;
import org.sonar.plugins.groovy.codenarc.CodeNarcSensor;
import org.sonar.plugins.groovy.codenarc.SonarWayProfile;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyColorizerFormat;
import org.sonar.plugins.groovy.foundation.GroovyCpdMapping;
import org.sonar.plugins.groovy.jacoco.JaCoCoExtensions;
import org.sonar.plugins.groovy.surefire.GroovySurefireParser;
import org.sonar.plugins.groovy.surefire.GroovySurefireSensor;

import java.util.List;

@Properties({
  @Property(
    key = GroovyPlugin.CODENARC_REPORT_PATH,
    name = "CodeNarc Report",
    description = "Path to the CodeNarc XML report. Path may be absolute or relative to the project base directory.",
    project = true,
    module = true,
    global = true),
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
    description =
    "If set to \"true\", the file headers (that are usually the same on each file: licensing information for example) are not considered as comments. " +
      "Thus metrics such as \"Comment lines\" do not get incremented. " +
      "If set to \"false\", those file headers are considered as comments and metrics such as \"Comment lines\" get incremented.",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN
  ),
  @Property(
    key = GroovyPlugin.FILE_SUFFIXES_KEY,
    defaultValue = GroovyPlugin.DEFAULT_FILE_SUFFIXES,
    name = "File suffixes",
    description = "Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.",
    project = true,
    module = true,
    global = true
  ),
  @Property(
    key = GroovyPlugin.SONAR_GROOVY_BINARIES,
    name = "Binary directories",
    description = "Comma-separated list of optional directories that contain the compiled groovy sources.",
    project = true,
    module = true,
    global = true)
})
public class GroovyPlugin extends SonarPlugin {

  public static final String CODENARC_REPORT_PATH = "sonar.groovy.codenarc.reportPath";
  public static final String COBERTURA_REPORT_PATH = "sonar.groovy.cobertura.reportPath";
  public static final String IGNORE_HEADER_COMMENTS = "sonar.groovy.ignoreHeaderComments";

  public static final String SONAR_GROOVY_BINARIES = "sonar.groovy.binaries";
  public static final String SONAR_GROOVY_BINARIES_FALLBACK = "sonar.binaries";

  public static final String FILE_SUFFIXES_KEY = "sonar.groovy.file.suffixes";
  public static final String DEFAULT_FILE_SUFFIXES = ".groovy";

  @Override
  public List getExtensions() {
    ImmutableList.Builder<Object> builder = ImmutableList.builder();
    builder.add(
      // CodeNarc
      CodeNarcRulesDefinition.class,
      CodeNarcSensor.class,
      SonarWayProfile.class,
      // Foundation
      Groovy.class,
      GroovyColorizerFormat.class,
      GroovyCpdMapping.class,
      // Main sensor
      GroovySensor.class,
      // Surefire
      GroovySurefireParser.class,
      GroovySurefireSensor.class,
      // Cobertura
      CoberturaSensor.class
      );
    builder.addAll(JaCoCoExtensions.getExtensions());
    return builder.build();
  }
}
