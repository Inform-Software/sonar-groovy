/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.groovy;

import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.plugins.groovy.codenarc.CodeNarcCheckTemplateRepository;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyColorizerFormat;
import org.sonar.plugins.groovy.foundation.GroovySourceImporter;

import java.util.ArrayList;
import java.util.List;

@Properties({
  @Property(
    key = GroovyPlugin.GMETRICS_REPORT_PATH,
    name = "Report file",
    description = "Path (absolute or relative) to GMetrics XML report in case generation is not handle by the plugin.",
    module = true,
    project = true,
    global = false
  ),
  @Property(
    key = GroovyPlugin.CODENARC_REPORT_PATH,
    name = "Report file",
    description = "Path (absolute or relative) to CodeNarc XML report in case generation is not handle by the plugin.",
    module = true,
    project = true,
    global = false
  )
})

  public class GroovyPlugin implements Plugin {
    public final static String GMETRICS_REPORT_PATH = "sonar.groovy.gmetrics.reportPath";
    public final static String CODENARC_REPORT_PATH = "sonar.groovy.codenarc.reportPath";

    public String getKey() {
      return Groovy.KEY;
    }

    public String getName() {
      return "Groovy";
    }

    public String getDescription() {
      return "Analysis of Groovy projects";
    }

    public List getExtensions() {
      List list = new ArrayList();

      // CodeNarc
      list.add(CodeNarcCheckTemplateRepository.class);
            
      // foundation
      list.add(Groovy.class);
      list.add(GroovyColorizerFormat.class);
      list.add(GroovySourceImporter.class);

      // Main sensor
      list.add(GroovySensor.class);
      return list;
    }
}
