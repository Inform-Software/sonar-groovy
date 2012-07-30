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

import org.codenarc.CodeNarcRunner;
import org.codenarc.analyzer.FilesystemSourceAnalyzer;
import org.codenarc.report.XmlReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

public class CodeNarcExecutor implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(CodeNarcExecutor.class);

  private RulesProfile rulesProfile;
  private CodeNarcProfileExporter profileExporter;
  private Project project;

  public CodeNarcExecutor(RulesProfile rulesProfile, CodeNarcProfileExporter profileExporter, Project project) {
    this.rulesProfile = rulesProfile;
    this.profileExporter = profileExporter;
    this.project = project;
  }

  /**
   * @return generated XML report
   */
  public File execute(File sourceDir) {
    LOG.info("Executing CodeNarc");

    CodeNarcRunner runner = new CodeNarcRunner();
    FilesystemSourceAnalyzer analyzer = new FilesystemSourceAnalyzer();

    // only one source directory
    analyzer.setBaseDirectory(sourceDir.getAbsolutePath());
    analyzer.setIncludes("**/*.groovy");
    runner.setSourceAnalyzer(analyzer);

    // generated XML report
    XmlReportWriter xmlReport = new XmlReportWriter();
    xmlReport.setTitle("Sonar");
    File reportFile = new File(project.getFileSystem().getSonarWorkingDirectory(), "codenarc-report.xml");
    xmlReport.setDefaultOutputFile(reportFile.getAbsolutePath());
    runner.setReportWriters(Arrays.asList(xmlReport));

    runner.setRuleSetFiles("file:" + getConfiguration().getAbsolutePath());

    // TODO : might be possible to process results object to get violations
    runner.execute();

    return reportFile;
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
