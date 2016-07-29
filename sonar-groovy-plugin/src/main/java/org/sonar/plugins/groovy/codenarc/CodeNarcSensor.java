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
package org.sonar.plugins.groovy.codenarc;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codenarc.CodeNarcRunner;
import org.codenarc.rule.Violation;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
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

  private static final Logger LOG = Loggers.get(CodeNarcSensor.class);

  private final FileSystem fileSystem;
  private final RulesProfile rulesProfile;
  private final GroovyFileSystem groovyFileSystem;

  private final String codeNarcReportPath;

  public CodeNarcSensor(Groovy groovy, FileSystem fileSystem, RulesProfile profile) {
    this.fileSystem = fileSystem;
    this.rulesProfile = profile;
    this.groovyFileSystem = new GroovyFileSystem(fileSystem);

    this.codeNarcReportPath = groovy.getCodeNarcReportPath();
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Groovy.KEY).name(this.toString());
  }

  @Override
  public void execute(SensorContext context) {
    if (shouldExecuteOnProject(context)) {
      analyse(context);
    }
  }

  @VisibleForTesting
  boolean shouldExecuteOnProject(SensorContext context) {
    return groovyFileSystem.hasGroovyFiles() && !context.activeRules().findByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY).isEmpty();
  }

  private void analyse(SensorContext context) {
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
      parseReport(context, Collections.singletonList(report));
    } else {
      // No, run CodeNarc
      runCodeNarc(context);
    }
  }

  private void parseReport(SensorContext context, List<File> reports) {
    for (File report : reports) {
      Collection<CodeNarcViolation> violations = CodeNarcXMLParser.parse(report, fileSystem);
      for (CodeNarcViolation violation : violations) {
        ActiveRule activeRule = context.activeRules().findByInternalKey(CodeNarcRulesDefinition.REPOSITORY_KEY, violation.getRuleName());
        if (activeRule != null) {
          InputFile inputFile = inputFileFor(violation.getFilename());
          insertIssue(context, violation, activeRule.ruleKey(), inputFile);
        } else {
          LOG.warn("No such rule in Sonar, so violation from CodeNarc will be ignored: ", violation.getRuleName());
        }
      }
    }
  }

  private static void insertIssue(SensorContext context, CodeNarcViolation violation, RuleKey ruleKey, @Nullable InputFile inputFile) {
    insertIssue(context, ruleKey, violation.getLine(), violation.getMessage(), inputFile);
  }

  private static void insertIssue(SensorContext context, RuleKey ruleKey, @Nullable Integer lineNumber, @Nullable String message, @Nullable InputFile inputFile) {
    if (inputFile != null) {
      NewIssue newIssue = context.newIssue().forRule(ruleKey);
      NewIssueLocation location = newIssue.newLocation().on(inputFile);
      if (lineNumber != null && lineNumber > 0) {
        location = location.at(inputFile.selectLine(lineNumber));
      }
      if (message != null) {
        location = location.message(message);
      }
      newIssue.at(location).save();
    }
  }

  private void runCodeNarc(SensorContext context) {
    LOG.info("Executing CodeNarc");

    File workdir = new File(fileSystem.workDir(), "/codenarc/");
    prepareWorkDir(workdir);
    File codeNarcConfiguration = new File(workdir, "profile.xml");
    exportCodeNarcConfiguration(codeNarcConfiguration);

    CodeNarcRunner runner = new CodeNarcRunner();
    runner.setRuleSetFiles("file:" + codeNarcConfiguration.getAbsolutePath());

    CodeNarcSourceAnalyzer analyzer = new CodeNarcSourceAnalyzer(groovyFileSystem.sourceFiles());
    runner.setSourceAnalyzer(analyzer);
    runner.execute();
    reportViolations(context, analyzer.getViolationsByFile());
  }

  private void reportViolations(SensorContext context, Map<File, List<Violation>> violationsByFile) {
    for (Entry<File, List<Violation>> violationsOnFile : violationsByFile.entrySet()) {
      InputFile groovyFile = inputFileFor(violationsOnFile.getKey().getAbsolutePath());
      if (groovyFile == null) {
        continue;
      }
      for (Violation violation : violationsOnFile.getValue()) {
        String ruleKey = violation.getRule().getName();
        ActiveRule activeRule = context.activeRules().findByInternalKey(CodeNarcRulesDefinition.REPOSITORY_KEY, ruleKey);
        if (activeRule != null) {
          insertIssue(context, activeRule.ruleKey(), violation.getLineNumber(), violation.getMessage(), groovyFile);
        } else {
          LOG.warn("No such rule in Sonar, so violation from CodeNarc will be ignored: ", ruleKey);
        }
      }
    }
  }

  @CheckForNull
  private InputFile inputFileFor(String path) {
    return fileSystem.inputFile(fileSystem.predicates().hasAbsolutePath(path));
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
