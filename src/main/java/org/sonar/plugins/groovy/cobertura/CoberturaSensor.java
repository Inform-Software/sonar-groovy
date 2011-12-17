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

package org.sonar.plugins.groovy.cobertura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.cobertura.api.AbstractCoberturaParser;
import org.sonar.plugins.cobertura.api.CoberturaUtils;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFile;

import java.io.File;

/**
 * TODO copied from sonar-cobertura-plugin with modifications: JavaFile replaced by GroovyFile, fixed SONARPLUGINS-696
 */
public class CoberturaSensor extends AbstractCoverageExtension implements Sensor, DependsUponMavenPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(CoberturaSensor.class);

  private CoberturaMavenPluginHandler handler;

  public CoberturaSensor(CoberturaMavenPluginHandler handler) {
    this.handler = handler;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return project.getAnalysisType().isDynamic(true) && Groovy.KEY.equals(project.getLanguageKey());
  }

  public void analyse(Project project, SensorContext context) {
    File report = CoberturaUtils.getReport(project);
    if (report != null) {
      parseReport(report, context);
    }
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)) {
      return handler;
    }
    return null;
  }

  protected void parseReport(File xmlFile, final SensorContext context) {
    LOG.info("parsing {}", xmlFile);
    COBERTURA_PARSER.parseReport(xmlFile, context);
  }

  private static final AbstractCoberturaParser COBERTURA_PARSER = new AbstractCoberturaParser() {
    @Override
    protected Resource<?> getResource(String fileName) {
      return new GroovyFile(fileName);
    }
  };

  @Override
  public String toString() {
    return "Groovy CoberturaSensor";
  }
}
