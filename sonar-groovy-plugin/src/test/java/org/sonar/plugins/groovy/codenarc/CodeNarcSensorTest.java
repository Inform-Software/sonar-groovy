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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeNarcSensorTest {

  private RulesProfile profile;
  private CodeNarcSensor sensor;
  private Groovy groovy;
  private SensorContextTester sensorContextTester;

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {

    sensorContextTester = SensorContextTester.create(temp.newFolder());
    sensorContextTester.fileSystem().setWorkDir(temp.newFolder());

    profile = mock(RulesProfile.class);

    sensorContextTester.setSettings(new Settings(new PropertyDefinitions(GroovyPlugin.class)));
    groovy = new Groovy(sensorContextTester.settings());
    sensor = new CodeNarcSensor(profile, new GroovyFileSystem(sensorContextTester.fileSystem()));
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Groovy.KEY);
  }

  @Test
  public void should_parse() throws Exception {

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "BooleanInstantiation");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "DuplicateImport");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyCatchBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyElseBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyFinallyBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyForStatement");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyIfStatement");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyTryBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyWhileStatement");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "ImportFromSamePackage");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "ReturnFromFinallyBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "StringInstantiation");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "ThrowExceptionFromFinallyBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "UnnecessaryGroovyImport");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "UnusedImport");
    sensorContextTester.setActiveRules(activeRulesBuilder.build());

    File reportUpdated = getReportWithUpdatedSourceDir();
    sensorContextTester.settings().setProperty(GroovyPlugin.CODENARC_REPORT_PATHS, reportUpdated.getAbsolutePath());

    addFileWithFakeContent("src/org/codenarc/sample/domain/SampleDomain.groovy");
    addFileWithFakeContent("src/org/codenarc/sample/service/NewService.groovy");
    addFileWithFakeContent("src/org/codenarc/sample/service/OtherService.groovy");
    addFileWithFakeContent("src/org/codenarc/sample/service/SampleService.groovy");

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(17);
  }

  @Test
  public void should_parse_but_not_add_issue_if_rule_not_found() throws Exception {

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "UnknownRule");
    sensorContextTester.setActiveRules(activeRulesBuilder.build());

    File reportUpdated = getReportWithUpdatedSourceDir();
    sensorContextTester.settings().setProperty(GroovyPlugin.CODENARC_REPORT_PATHS, reportUpdated.getAbsolutePath());

    addFileWithFakeContent("src/org/codenarc/sample/domain/SampleDomain.groovy");
    addFileWithFakeContent("src/org/codenarc/sample/service/NewService.groovy");
    addFileWithFakeContent("src/org/codenarc/sample/service/OtherService.groovy");
    addFileWithFakeContent("src/org/codenarc/sample/service/SampleService.groovy");

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }

  @Test
  public void should_parse_but_not_add_issue_if_inputFile_not_found() throws Exception {

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "BooleanInstantiation");
    sensorContextTester.setActiveRules(activeRulesBuilder.build());

    File reportUpdated = getReportWithUpdatedSourceDir();
    sensorContextTester.settings().setProperty(GroovyPlugin.CODENARC_REPORT_PATHS, reportUpdated.getAbsolutePath());

    addFileWithFakeContent("src/org/codenarc/sample/domain/Unknown.groovy");

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }

  @Test
  public void should_run_code_narc() throws IOException {

    addFileWithContent("src/sample.groovy", "package source\nclass SourceFile1 {\n}");

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateRule(activeRulesBuilder, "org.codenarc.rule.basic.EmptyClassRule", "EmptyClass");
    sensorContextTester.setActiveRules(activeRulesBuilder.build());

    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRuleKey()).thenReturn("org.codenarc.rule.basic.EmptyClassRule");
    when(profile.getActiveRulesByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY)).thenReturn(Arrays.asList(activeRule));

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(1);
  }

  @Test
  public void should_do_nothing_when_can_not_find_report_path() throws Exception {

    sensorContextTester.settings().setProperty(GroovyPlugin.CODENARC_REPORT_PATHS, "../missing_file.xml");

    addFileWithFakeContent("src/org/codenarc/sample/domain/Unknown.groovy");

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateRule(activeRulesBuilder, "org.codenarc.rule.basic.EmptyClassRule", "EmptyClass");
    sensorContextTester.setActiveRules(activeRulesBuilder.build());

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }

  @Test
  public void should_run_code_narc_with_multiple_files() throws IOException {

    addFileWithContent("src/sample.groovy", "package source\nclass SourceFile1 {\n}");
    addFileWithContent("src/foo/bar/qix/sample.groovy", "package source\nclass SourceFile1 {\n}");

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateRule(activeRulesBuilder, "org.codenarc.rule.basic.EmptyClassRule", "EmptyClass");
    sensorContextTester.setActiveRules(activeRulesBuilder.build());

    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRuleKey()).thenReturn("org.codenarc.rule.basic.EmptyClassRule");
    when(profile.getActiveRulesByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY)).thenReturn(Arrays.asList(activeRule));

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(2);
  }

  private File getReportWithUpdatedSourceDir() throws IOException {
    File report = FileUtils.toFile(getClass().getResource("parsing/sample.xml"));
    File reportUpdated = temp.newFile();
    FileUtils.write(reportUpdated,
      FileUtils.readFileToString(report).replaceAll(Pattern.quote("[sourcedir]"), sensorContextTester.fileSystem().baseDir().toPath().resolve("src").toAbsolutePath().toString()));
    return reportUpdated;
  }

  private void addFileWithFakeContent(String path) throws UnsupportedEncodingException, IOException {
    File sampleFile = FileUtils.toFile(getClass().getResource("parsing/Sample.groovy"));
    sensorContextTester.fileSystem().add(new DefaultInputFile(sensorContextTester.module().key(), path)
      .setLanguage(Groovy.KEY)
      .setType(Type.MAIN)
      .initMetadata(new String(Files.readAllBytes(sampleFile.toPath()), "UTF-8")));
  }

  private void addFileWithContent(String path, String content) throws UnsupportedEncodingException, IOException {
    DefaultInputFile inputFile = new DefaultInputFile(sensorContextTester.module().key(), path)
      .setLanguage(Groovy.KEY)
      .setType(Type.MAIN)
      .initMetadata(content);
    sensorContextTester.fileSystem().add(inputFile);
    FileUtils.write(inputFile.file(), content, StandardCharsets.UTF_8);
  }

  private static ActiveRulesBuilder activateFakeRule(ActiveRulesBuilder activeRulesBuilder, String ruleKey) {
    return activateRule(activeRulesBuilder, ruleKey, ruleKey);
  }

  private static ActiveRulesBuilder activateRule(ActiveRulesBuilder activeRulesBuilder, String ruleKey, String internalKey) {
    return activeRulesBuilder.create(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY, ruleKey)).setInternalKey(internalKey).activate();
  }

}
