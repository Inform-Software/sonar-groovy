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
package org.sonar.plugins.groovy.surefire;

import java.io.File;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
        return new DefaultInputFile("", (String) invocation.getArguments()[0]);
      }
    }).when(parser).getUnitTestInputFile(anyString());
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(any(TestCase.Status.class))).thenReturn(testCase);
    when(testCase.setMessage(ArgumentMatchers.nullable(String.class))).thenReturn(testCase);
    when(testCase.setStackTrace(anyString())).thenReturn(testCase);
    when(testCase.setType(anyString())).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    when(perspectives.as(eq(MutableTestPlan.class),
      argThat(inputFileMatcher(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")))).thenReturn(testPlan);

    parser.collect(context, getDir("multipleReports"));

    verify(testPlan).addTestCase("testGetUnKnownCollector");
    verify(testPlan).addTestCase("testGetJDependsCollector");
  }

  private static ArgumentMatcher<InputFile> inputFileMatcher(final String fileName) {
    return new ArgumentMatcher<InputFile>() {
      @Override
      public boolean matches(InputFile arg0) {
        return fileName.equals(arg0.key());
      }
    };
  }

  @Test
  public void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() throws Exception {
    parser.collect(context, null);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDir("nonExistingReportsDirectory"));
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDir("file.txt"));
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldAggregateReports() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));

    parser.collect(context, getDir("multipleReports"));

    // Only 6 tests measures should be stored, no more: the TESTS-AllTests.xml must not be read as there's 1 file result per unit test
    // (SONAR-2841).
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(6);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(6);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(6);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(6);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(6);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(6);
  }

  // SONAR-2841: if there's only a test suite report, then it should be read.
  @Test
  public void shouldUseTestSuiteReportIfAlone() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));

    parser.collect(context, getDir("onlyTestSuiteReport"));

    assertThat(context.measures(":org.sonar.SecondTest")).hasSize(6);
    assertThat(context.measures(":org.sonar.JavaNCSSCollectorTest")).hasSize(6);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldInsertZeroWhenNoReports() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDir("noReports"));
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldNotInsertZeroOnFiles() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDir("noTests"));

    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldMergeInnerClasses() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    parser.collect(context, getDir("innerClasses"));

    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measures(":org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")).isEmpty();
  }

  @Test
  public void shouldMergeNestedInnerClasses() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    parser.collect(context, getDir("nestedInnerClasses"));

    assertThat(context.measure(":org.sonar.plugins.surefire.NestedInnerTest", CoreMetrics.TESTS).value()).isEqualTo(3);
  }

  @Test
  public void should_not_count_negative_tests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    parser.collect(context, getDir("negativeTestTime"));
    // Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.

    assertThat(context.measure(":java.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TESTS).value()).isEqualTo(6);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(659);
  }

  private java.io.File getDir(String dirname) throws URISyntaxException {
    return new java.io.File("src/test/resources/org/sonar/plugins/groovy/surefire/SurefireParserTest/" + dirname);
  }

  @Test
  public void should_generate_correct_predicate() throws URISyntaxException {
    DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    DefaultInputFile inputFile = new DefaultInputFile("", "src/test/org/sonar/JavaNCSSCollectorTest.groovy")
      .setLanguage(Groovy.KEY)
      .setType(Type.TEST);
    fs.add(inputFile);

    parser = new GroovySurefireParser(groovy, perspectives, fs);

    SensorContextTester context = SensorContextTester.create(new File(""));
    context.setFileSystem(fs);
    parser.collect(context, getDir("onlyTestSuiteReport"));

    assertThat(context.measure(":src/test/org/sonar/JavaNCSSCollectorTest.groovy", CoreMetrics.TESTS).value()).isEqualTo(11);
  }

}
