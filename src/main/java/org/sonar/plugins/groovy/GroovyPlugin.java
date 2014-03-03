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

import com.google.common.collect.ImmutableList;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.groovy.cobertura.CoberturaSensor;
import org.sonar.plugins.groovy.codenarc.CodeNarcRuleRepository;
import org.sonar.plugins.groovy.codenarc.CodeNarcSensor;
import org.sonar.plugins.groovy.codenarc.SonarWayProfile;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyColorizerFormat;
import org.sonar.plugins.groovy.foundation.GroovyCpdMapping;
import org.sonar.plugins.groovy.foundation.GroovySourceImporter;

import java.util.List;

@Properties({
  @Property(
    key = GroovyPlugin.CODENARC_REPORT_PATH,
    name = "CodeNarc Report",
    description = "Path to the CodeNarc XML report. Path may be absolute or relative to the project base directory.",
    module = true,
    project = true,
    global = true
  ),
  @Property(
    key = GroovyPlugin.COBERTURA_REPORT_PATH,
    name = "Cobertura Report",
    description = "Path to the Cobertura XML report. Path may be absolute or relative to the project base directory.",
    module = true,
    global = true,
    project = true),
  @Property(
    key = GroovyPlugin.IGNORE_HEADER_COMMENTS,
    defaultValue = "true",
    name = "Ignore Header Comments",
    description =
    "If set to \"true\", the file headers (that are usually the same on each file: licensing information for example) are not considered as comments. " +
      "Thus metrics such as \"Comment lines\" do not get incremented. " +
      "If set to \"false\", those file headers are considered as comments and metrics such as \"Comment lines\" get incremented.",
    project = true, global = true,
    type = PropertyType.BOOLEAN)
})
public class GroovyPlugin extends SonarPlugin {

  public static final String CODENARC_REPORT_PATH = "sonar.groovy.codenarc.reportPath";
  public static final String COBERTURA_REPORT_PATH = "sonar.groovy.cobertura.reportPath";
  public static final String IGNORE_HEADER_COMMENTS = "sonar.groovy.ignoreHeaderComments";

  public List getExtensions() {
    return ImmutableList.of(
      GroovyCommonRulesDecorator.class,
      GroovyCommonRulesEngine.class,
      // CodeNarc
      CodeNarcRuleRepository.class,
      CodeNarcSensor.class,
      SonarWayProfile.class,
      // Foundation
      Groovy.class,
      GroovyColorizerFormat.class,
      GroovySourceImporter.class,
      GroovyCpdMapping.class,
      // Main sensor
      GroovySensor.class,

      // Cobertura
      CoberturaSensor.class
      );
  }

}
