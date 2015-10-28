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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.test.IsResource;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
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
public class GroovySurefireParserTest {

  private ResourcePerspectives perspectives;
  private FileSystem fs;
  private GroovySurefireParser parser;
  private Groovy groovy;
  private SensorContext context;

  @Before
  public void before() {
    context = mock(SensorContext.class);
    perspectives = mock(ResourcePerspectives.class);
    fs = new DefaultFileSystem(new File("."));

    Settings settings = mock(Settings.class);
    when(settings.getStringArray(GroovyPlugin.FILE_SUFFIXES_KEY)).thenReturn(new String[] {".groovy", "grvy"});
    groovy = new Groovy(settings);

    parser = spy(new GroovySurefireParser(groovy, perspectives, fs));

    doAnswer(new Answer<InputFile>() {
      @Override
      public InputFile answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultInputFile((String) invocation.getArguments()[0]);
      }
    }).when(parser).getUnitTestInputFile(anyString());
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(any(TestCase.Status.class))).thenReturn(testCase);
    when(testCase.setMessage(anyString())).thenReturn(testCase);
    when(testCase.setStackTrace(anyString())).thenReturn(testCase);
    when(testCase.setType(anyString())).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    when(perspectives.as(eq(MutableTestPlan.class),
      argThat(inputFileMatcher("ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")))).thenReturn(testPlan);

    parser.collect(context, getDir("multipleReports"));

    verify(testPlan).addTestCase("testGetUnKnownCollector");
    verify(testPlan).addTestCase("testGetJDependsCollector");
  }

  private static BaseMatcher<InputFile> inputFileMatcher(final String fileName) {
    return new BaseMatcher<InputFile>() {
      @Override
      public boolean matches(Object arg0) {
        return fileName.equals(((InputFile) arg0).relativePath());
      }

      @Override
      public void describeTo(Description arg0) {
      }
    };
  }

  @Test
  public void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() throws Exception {
    parser.collect(context, null);
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());

    context = mock(SensorContext.class);
    parser.collect(context, getDir("nonExistingReportsDirectory"));
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());

    context = mock(SensorContext.class);
    parser.collect(context, getDir("file.txt"));
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());
  }

  @Test
  public void shouldAggregateReports() throws URISyntaxException {
    parser.collect(context, getDir("multipleReports"));

    // Only 6 tests measures should be stored, no more: the TESTS-AllTests.xml must not be read as there's 1 file result per unit test
    // (SONAR-2841).
    verify(context, times(6)).saveMeasure(any(InputFile.class), eq(CoreMetrics.SKIPPED_TESTS), eq(0.0));
    verify(context, times(6)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(6)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TEST_ERRORS), anyDouble());
  }

  // SONAR-2841: if there's only a test suite report, then it should be read.
  @Test
  public void shouldUseTestSuiteReportIfAlone() throws URISyntaxException {
    parser.collect(context, getDir("onlyTestSuiteReport"));

    verify(context, times(2)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(2)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TEST_ERRORS), anyDouble());
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldInsertZeroWhenNoReports() throws URISyntaxException {
    parser.collect(context, getDir("noReports"));
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldNotInsertZeroWhenNoReports() throws URISyntaxException {
    parser.collect(context, getDir("noReports"));

    verify(context, never()).saveMeasure(CoreMetrics.TESTS, 0.0);
  }

  @Test
  public void shouldNotInsertZeroOnFiles() throws URISyntaxException {
    parser.collect(context, getDir("noTests"));

    verify(context, never()).saveMeasure(any(Resource.class), any(Metric.class), anyDouble());
  }

  @Test
  public void shouldMergeInnerClasses() throws URISyntaxException {
    parser.collect(context, getDir("innerClasses"));

    verify(context)
      .saveMeasure(argThat(inputFileMatcher("org.apache.commons.collections.bidimap.AbstractTestBidiMap")), eq(CoreMetrics.TESTS), eq(7.0));
    verify(context).saveMeasure(argThat(inputFileMatcher("org.apache.commons.collections.bidimap.AbstractTestBidiMap")), eq(CoreMetrics.TEST_ERRORS),
      eq(1.0));
    verify(context, never()).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")),
      any(Metric.class), anyDouble());
  }

  @Test
  public void shouldMergeNestedInnerClasses() throws URISyntaxException {
    parser.collect(context, getDir("nestedInnerClasses"));

    verify(context).saveMeasure(
      argThat(inputFileMatcher("org.sonar.plugins.surefire.NestedInnerTest")),
      eq(CoreMetrics.TESTS),
      eq(3.0));
  }

  @Test
  public void should_not_count_negative_tests() throws URISyntaxException {
    parser.collect(context, getDir("negativeTestTime"));
    // Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.

    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.SKIPPED_TESTS), eq(0.0));
    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TEST_ERRORS), anyDouble());
    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TEST_FAILURES), anyDouble());
    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TEST_EXECUTION_TIME), eq(659.0));
  }

  private java.io.File getDir(String dirname) throws URISyntaxException {
    return new java.io.File("src/test/resources/org/sonar/plugins/groovy/surefire/SurefireParserTest/" + dirname);
  }

  @Test
  public void should_generate_correct_predicate() throws URISyntaxException {
    DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    fs.add(new DefaultInputFile("src/test/org/sonar/JavaNCSSCollectorTest.groovy")
      .setAbsolutePath("src/test/org/sonar/JavaNCSSCollectorTest.groovy")
      .setLanguage(Groovy.KEY)
      .setType(Type.TEST));

    parser = new GroovySurefireParser(groovy, perspectives, fs);

    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDir("onlyTestSuiteReport"));

    verify(context, times(1)).saveMeasure(any(InputFile.class), eq(CoreMetrics.TESTS), anyDouble());
  }

}
