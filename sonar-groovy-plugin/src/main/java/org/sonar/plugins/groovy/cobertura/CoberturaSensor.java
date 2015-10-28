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
package org.sonar.plugins.groovy.cobertura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

import java.io.File;

public class CoberturaSensor implements Sensor, CoverageExtension {

  private static final Logger LOG = LoggerFactory.getLogger(CoberturaSensor.class);

  private final Settings settings;
  private final FileSystem fileSystem;
  private final GroovyFileSystem groovyFileSystem;

  public CoberturaSensor(Settings settings, FileSystem fileSystem) {
    this.settings = settings;
    this.fileSystem = fileSystem;
    this.groovyFileSystem = new GroovyFileSystem(fileSystem);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return groovyFileSystem.hasGroovyFiles();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    String reportPath = settings.getString(GroovyPlugin.COBERTURA_REPORT_PATH);

    if (reportPath != null) {
      File xmlFile = new File(reportPath);
      if (!xmlFile.isAbsolute()) {
        xmlFile = new File(fileSystem.baseDir(), reportPath);
      }
      if (xmlFile.exists()) {
        LOG.info("Analyzing Cobertura report: " + reportPath);
        new CoberturaReportParser(context, fileSystem).parseReport(xmlFile);
      } else {
        LOG.info("Cobertura xml report not found: " + reportPath);
      }
    } else {
      LOG.info("No Cobertura report provided (see '" + GroovyPlugin.COBERTURA_REPORT_PATH + "' property)");
    }
  }

  @Override
  public String toString() {
    return "Groovy CoberturaSensor";
  }

}
