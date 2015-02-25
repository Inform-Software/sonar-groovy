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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CodeNarcSensorTest {

  private RuleFinder ruleFinder;
  private RulesProfile profile;
  private CodeNarcSensor sensor;
  private Settings settings;
  private ModuleFileSystem moduleFileSystem;
  private DefaultFileSystem fileSystem;

  @org.junit.Rule
  public TemporaryFolder projectdir = new TemporaryFolder();

  @Before
  public void setUp() {
    ruleFinder = mock(RuleFinder.class);
    profile = mock(RulesProfile.class);
    settings = mock(Settings.class);
    moduleFileSystem = mock(ModuleFileSystem.class);
    fileSystem = new DefaultFileSystem();
    sensor = new CodeNarcSensor(settings, moduleFileSystem, fileSystem, profile, ruleFinder);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    fileSystem.add(new DefaultInputFile("fake.groovy").setLanguage(Groovy.KEY));
    when(profile.getActiveRulesByRepository(CodeNarcRuleRepository.REPOSITORY_KEY))
      .thenReturn(Arrays.asList(new ActiveRule()));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_when_no_active_rules() {
    Project project = mock(Project.class);
    fileSystem.add(new DefaultInputFile("fake.groovy").setLanguage(Groovy.KEY));
    when(profile.getActiveRulesByRepository(CodeNarcRuleRepository.REPOSITORY_KEY))
      .thenReturn(Collections.EMPTY_LIST);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_if_no_groovy_files() {
    Project project = mock(Project.class);
    when(profile.getActiveRulesByRepository(CodeNarcRuleRepository.REPOSITORY_KEY))
      .thenReturn(Arrays.asList(new ActiveRule()));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_parse() {
    when(ruleFinder.find(Mockito.any(RuleQuery.class))).thenReturn(Rule.create());

    Project project = mock(Project.class);
    File report = FileUtils.toFile(getClass().getResource("CodeNarcXMLParserTest/sample.xml"));
    when(settings.getString(GroovyPlugin.CODENARC_REPORT_PATH)).thenReturn(report.getAbsolutePath());

    SensorContext context = mock(SensorContext.class);
    sensor.analyse(project, context);

    verify(context, atLeastOnce()).saveViolation(Mockito.any(Violation.class));
  }

  @Test
  public void should_run_code_narc() throws IOException {
    File sonarhome = projectdir.newFolder("sonarhome");
    PrintWriter pw = new PrintWriter(new File(sonarhome, "sample.groovy"));
    pw.write("package source\nclass SourceFile1 {\n}");
    pw.close();
    when(ruleFinder.find(Mockito.any(RuleQuery.class))).thenReturn(Rule.create());

    Project project = mock(Project.class);
    ActiveRule rule = mock(ActiveRule.class);
    when(rule.getRuleKey()).thenReturn("org.codenarc.rule.basic.EmptyClassRule");
    when(profile.getActiveRulesByRepository(CodeNarcRuleRepository.REPOSITORY_KEY))
      .thenReturn(Arrays.asList(rule));
    when(settings.getString(GroovyPlugin.CODENARC_REPORT_PATH)).thenReturn("");
    when(moduleFileSystem.workingDir()).thenReturn(sonarhome);
    when(moduleFileSystem.sourceDirs()).thenReturn(Lists.newArrayList(sonarhome));

    SensorContext context = mock(SensorContext.class);
    sensor.analyse(project, context);

    verify(context, atLeastOnce()).saveViolation(Mockito.any(Violation.class));
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("CodeNarc");
  }

}
