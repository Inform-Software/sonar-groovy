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

package org.sonar.plugins.groovy.codenarc;

import org.codenarc.analyzer.FilesystemSourceAnalyzer;
import org.codenarc.report.XmlReportWriter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.groovy.utils.GroovyUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

public class CodeNarcRunner {

  private RulesProfile rulesProfile;
  private CodeNarcProfileExporter profileExporter;
  private Project project;

  public CodeNarcRunner(RulesProfile rulesProfile, CodeNarcProfileExporter profileExporter, Project project) {
    this.rulesProfile = rulesProfile;
    this.profileExporter = profileExporter;
    this.project = project;
  }

  public void execute(File sourceDir) {
    GroovyUtils.LOG.info("Executing CodeNarc");

    org.codenarc.CodeNarcRunner runner = new org.codenarc.CodeNarcRunner();
    FilesystemSourceAnalyzer analyzer = new FilesystemSourceAnalyzer();

    // only one source directory
    analyzer.setBaseDirectory(sourceDir.getAbsolutePath());
    analyzer.setIncludes("**/*.groovy");
    runner.setSourceAnalyzer(analyzer);

    // generated xml report
    XmlReportWriter xmlReport = new XmlReportWriter();
    xmlReport.setTitle("Sonar");
    xmlReport.setDefaultOutputFile(new File(project.getFileSystem().getSonarWorkingDirectory(), "codenarc-report.xml").getAbsolutePath());
    runner.setReportWriters(Arrays.asList(xmlReport));

    runner.setRuleSetFiles("file:" + getConfiguration().getAbsolutePath());

    // TODO : might be possible to process results object to get violations
    runner.execute();
  }

  private File getConfiguration() {
    try {
      StringWriter writer = new StringWriter();
      profileExporter.exportProfile(rulesProfile, writer);
      return project.getFileSystem().writeToWorkingDirectory(writer.toString(), "checkProfile.xml");
    } catch (IOException e) {
      throw new SonarException("Can not generate CodeNarc configuration file", e);
    }
  }
}
