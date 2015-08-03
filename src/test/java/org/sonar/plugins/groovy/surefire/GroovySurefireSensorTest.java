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
package org.sonar.plugins.groovy.surefire;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.surefire.api.SurefireUtils;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by iwarapter
 */
public class GroovySurefireSensorTest {

  private DefaultFileSystem fs = new DefaultFileSystem(new File("."));
  private ResourcePerspectives perspectives;
  private GroovySurefireSensor surefireSensor;
  private PathResolver pathResolver = new PathResolver();
  private Groovy groovy;
  private SensorContext context;

  @Before
  public void before() {
    fs = new DefaultFileSystem(new File("."));
    DefaultInputFile groovyFile = new DefaultInputFile("src/org/foo/grvy");
    groovyFile.setLanguage(Groovy.KEY);
    fs.add(groovyFile);
    perspectives = mock(ResourcePerspectives.class);

    context = mock(SensorContext.class);

    Settings settings = mock(Settings.class);
    when(settings.getStringArray(GroovyPlugin.FILE_SUFFIXES_KEY)).thenReturn(new String[] {".groovy", "grvy"});
    groovy = new Groovy(settings);

    GroovySurefireParser parser = spy(new GroovySurefireParser(groovy, perspectives, fs));

    doAnswer(new Answer<InputFile>() {
      @Override
      public InputFile answer(InvocationOnMock invocation) throws Throwable {
        return inputFile((String) invocation.getArguments()[0]);
      }
    }).when(parser).getUnitTestInputFile(anyString());

    surefireSensor = new GroovySurefireSensor(parser, mock(Settings.class), fs, pathResolver);
  }

  @Test
  public void should_execute_if_filesystem_contains_groovy_files() {
    surefireSensor = new GroovySurefireSensor(new GroovySurefireParser(groovy, perspectives, fs), mock(Settings.class), fs, pathResolver);
    Assertions.assertThat(surefireSensor.shouldExecuteOnProject(mock(Project.class))).isTrue();
  }

  @Test
  public void should_not_execute_if_filesystem_does_not_contains_groovy_files() {
    surefireSensor = new GroovySurefireSensor(new GroovySurefireParser(groovy, perspectives, fs), mock(Settings.class), new DefaultFileSystem(new File(".")), pathResolver);
    Assertions.assertThat(surefireSensor.shouldExecuteOnProject(mock(Project.class))).isFalse();
  }

  @Test
  public void shouldNotFailIfReportsNotFound() {
    Settings settings = mock(Settings.class);
    when(settings.getString(SurefireUtils.SUREFIRE_REPORTS_PATH_PROPERTY)).thenReturn("unknown");

    ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
    when(projectFileSystem.resolvePath("unknown")).thenReturn(new File("src/test/resources/unknown"));

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(projectFileSystem);

    GroovySurefireSensor surefireSensor = new GroovySurefireSensor(mock(GroovySurefireParser.class), settings, fs, pathResolver);
    surefireSensor.analyse(project, context);
  }

  @Test
  public void shouldHandleTestSuiteDetails() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/shouldHandleTestSuiteDetails/").toURI()));

    // 3 classes, 6 measures by class
    verify(context, times(3)).saveMeasure(any(InputFile.class), eq(CoreMetrics.SKIPPED_TESTS), anyDouble());
    verify(context, times(3)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(18)).saveMeasure(any(InputFile.class), any(Metric.class), anyDouble());

    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest")), eq(CoreMetrics.TESTS), eq(4d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest")), eq(CoreMetrics.TEST_EXECUTION_TIME),
      eq(111d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest")), eq(CoreMetrics.TEST_FAILURES), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest")), eq(CoreMetrics.TEST_ERRORS), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest")), eq(CoreMetrics.SKIPPED_TESTS), eq(0d));

    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest2")), eq(CoreMetrics.TESTS), eq(2d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest2")), eq(CoreMetrics.TEST_EXECUTION_TIME), eq(2d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest2")), eq(CoreMetrics.TEST_FAILURES), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest2")), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest2")), eq(CoreMetrics.SKIPPED_TESTS), eq(0d));

    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest3")), eq(CoreMetrics.TESTS), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest3")), eq(CoreMetrics.TEST_EXECUTION_TIME),
      eq(16d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest3")), eq(CoreMetrics.TEST_FAILURES), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest3")), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.core.ExtensionsFinderTest3")), eq(CoreMetrics.SKIPPED_TESTS), eq(1d));

  }

  @Test
  public void shouldSaveErrorsAndFailuresInXML() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/shouldSaveErrorsAndFailuresInXML/").toURI()));

    // 1 classes, 6 measures by class
    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.SKIPPED_TESTS), anyDouble());
    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(6)).saveMeasure(any(InputFile.class), any(Metric.class), anyDouble());
  }

  @Test
  public void shouldManageClassesWithDefaultPackage() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/shouldManageClassesWithDefaultPackage/").toURI()));

    verify(context).saveMeasure(inputFile("NoPackagesTest"), CoreMetrics.TESTS, 2d);
  }

  @Test
  public void successRatioIsZeroWhenAllTestsFail() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/successRatioIsZeroWhenAllTestsFail/").toURI()));

    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TESTS), eq(2d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_FAILURES), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_ERRORS), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_SUCCESS_DENSITY), eq(0d));
  }

  @Test
  public void measuresShouldNotIncludeSkippedTests() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/measuresShouldNotIncludeSkippedTests/").toURI()));

    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TESTS), eq(2d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_FAILURES), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.SKIPPED_TESTS), eq(1d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_SUCCESS_DENSITY), eq(50d));
  }

  @Test
  public void noSuccessRatioIfNoTests() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/noSuccessRatioIfNoTests/").toURI()));

    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TESTS), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_FAILURES), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.SKIPPED_TESTS), eq(2d));
    verify(context, never()).saveMeasure(eq(inputFile("org.sonar.Foo")), eq(CoreMetrics.TEST_SUCCESS_DENSITY), anyDouble());
  }

  @Test
  public void ignoreSuiteAsInnerClass() throws URISyntaxException {
    surefireSensor.collect(context, new File(getClass().getResource(
      "/org/sonar/plugins/groovy/surefire/SurefireSensorTest/ignoreSuiteAsInnerClass/").toURI()));

    // ignore TestHandler$Input.xml
    verify(context).saveMeasure(eq(inputFile("org.apache.shindig.protocol.TestHandler")), eq(CoreMetrics.TESTS), eq(0.0));
    verify(context).saveMeasure(eq(inputFile("org.apache.shindig.protocol.TestHandler")), eq(CoreMetrics.SKIPPED_TESTS), eq(1.0));
  }

  private static InputFile inputFile(String key) {
    return new DefaultInputFile(key).setType(InputFile.Type.TEST);
  }
}
