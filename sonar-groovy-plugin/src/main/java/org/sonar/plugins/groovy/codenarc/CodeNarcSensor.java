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
package org.sonar.plugins.groovy.codenarc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codenarc.CodeNarcRunner;
import org.codenarc.rule.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.codenarc.CodeNarcXMLParser.CodeNarcViolation;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CodeNarcSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(CodeNarcSensor.class);

  private final ResourcePerspectives perspectives;
  private final FileSystem fileSystem;
  private final RulesProfile rulesProfile;
  private final RuleFinder ruleFinder;
  private final GroovyFileSystem groovyFileSystem;

  private final String codeNarcReportPath;

  public CodeNarcSensor(
    Groovy groovy,
    ResourcePerspectives perspectives,
    FileSystem fileSystem,
    RulesProfile profile,
    RuleFinder ruleFinder) {
    this.perspectives = perspectives;
    this.fileSystem = fileSystem;
    this.rulesProfile = profile;
    this.ruleFinder = ruleFinder;
    this.groovyFileSystem = new GroovyFileSystem(fileSystem);

    this.codeNarcReportPath = groovy.getCodeNarcReportPath();
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return groovyFileSystem.hasGroovyFiles() && !rulesProfile.getActiveRulesByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY).isEmpty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    // Should we reuse existing report from CodeNarc ?
    if (StringUtils.isNotBlank(codeNarcReportPath)) {
      // Yes
      File report = new File(codeNarcReportPath);
      if (!report.isAbsolute()) {
        report = new File(fileSystem.baseDir(), codeNarcReportPath);
      }
      if (!report.isFile()) {
        LOG.warn("Groovy report " + GroovyPlugin.CODENARC_REPORT_PATH + " not found at {}", report);
        return;
      }
      parseReport(Collections.singletonList(report));
    } else {
      // No, run CodeNarc
      runCodeNarc();
    }
  }

  private void parseReport(List<File> reports) {
    for (File report : reports) {
      Collection<CodeNarcViolation> violations = CodeNarcXMLParser.parse(report, fileSystem);
      for (CodeNarcViolation violation : violations) {
        RuleQuery ruleQuery = RuleQuery.create().withRepositoryKey(CodeNarcRulesDefinition.REPOSITORY_KEY).withConfigKey(violation.getRuleName());
        Rule rule = ruleFinder.find(ruleQuery);
        if (rule != null) {
          insertIssue(violation, rule, issuableFor(violation.getFilename()));
        } else {
          LOG.warn("No such rule in Sonar, so violation from CodeNarc will be ignored: ", violation.getRuleName());
        }
      }
    }
  }

  private static void insertIssue(CodeNarcViolation violation, Rule rule, @Nullable Issuable issuable) {
    if (issuable != null) {
      insertIssue(rule.ruleKey(), violation.getLine(), violation.getMessage(), issuable);
    }
  }

  private static void insertIssue(RuleKey ruleKey, Integer line, String message, Issuable issuable) {
    Issue issue = issuable.newIssueBuilder()
      .ruleKey(ruleKey)
      .line(line)
      .message(message)
      .build();
    issuable.addIssue(issue);
  }

  private void runCodeNarc() {
    LOG.info("Executing CodeNarc");

    File workdir = new File(fileSystem.workDir(), "/codenarc/");
    prepareWorkDir(workdir);
    File codeNarcConfiguration = new File(workdir, "profile.xml");
    exportCodeNarcConfiguration(codeNarcConfiguration);

    CodeNarcRunner runner = new CodeNarcRunner();
    runner.setRuleSetFiles("file:" + codeNarcConfiguration.getAbsolutePath());

    List<File> sourceFiles = groovyFileSystem.sourceFiles();
    CodeNarcSourceAnalyzer analyzer = new CodeNarcSourceAnalyzer(sourceFiles, fileSystem.baseDir());
    runner.setSourceAnalyzer(analyzer);
    runner.execute();
    reportViolations(analyzer.getViolationsByFile());
  }

  private void reportViolations(Map<File, List<Violation>> violationsByFile) {
    for (Entry<File, List<Violation>> violationsOnFile : violationsByFile.entrySet()) {
      Issuable issuable = issuableFor(violationsOnFile.getKey().getAbsolutePath());
      if (issuable == null) {
        continue;
      }
      for (Violation violation : violationsOnFile.getValue()) {
        String ruleName = violation.getRule().getName();
        RuleQuery ruleQuery = RuleQuery.create().withRepositoryKey(CodeNarcRulesDefinition.REPOSITORY_KEY).withConfigKey(ruleName);
        Rule rule = ruleFinder.find(ruleQuery);
        if (rule != null) {
          insertIssue(rule.ruleKey(), violation.getLineNumber(), violation.getMessage(), issuable);
        } else {
          LOG.warn("No such rule in Sonar, so violation from CodeNarc will be ignored: ", ruleName);
        }
      }
    }
  }

  @CheckForNull
  private Issuable issuableFor(String path) {
    InputFile sonarFile = fileSystem.inputFile(fileSystem.predicates().hasAbsolutePath(path));
    if (sonarFile != null) {
      return perspectives.as(Issuable.class, sonarFile);
    }
    return null;
  }

  private void exportCodeNarcConfiguration(File file) {
    try {
      StringWriter writer = new StringWriter();
      new CodeNarcProfileExporter(writer).exportProfile(rulesProfile);
      FileUtils.writeStringToFile(file, writer.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Can not generate CodeNarc configuration file", e);
    }
  }

  private static void prepareWorkDir(File dir) {
    try {
      FileUtils.forceMkdir(dir);
      // directory is cleaned, because Sonar 3.0 will not do this for us
      FileUtils.cleanDirectory(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create directory: " + dir, e);
    }
  }

  @Override
  public String toString() {
    return "CodeNarc";
  }

}
