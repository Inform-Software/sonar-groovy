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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;

import java.io.File;

public class CodeNarcXMLParserTest {

  @Test
  public void testCodeNarcReportParser() {
    File fileToParse = FileUtils.toFile(getClass().getResource("CodeNarcXMLParserTest/sample.xml"));

    CodeNarcXMLParser parser = new CodeNarcXMLParser(null);
    parser = spy(parser);
    doNothing().when(parser).log((SensorContext) anyObject(), anyString(), anyString(), anyInt(), anyString());

    SensorContext context = mock(SensorContext.class);
    parser.parseAndLogCodeNarcResults(fileToParse, context);

    verify(parser).log(eq(context), eq("EmptyIfStatement"), eq("org/codenarc/sample/domain/SampleDomain"), eq(21), eq(""));
    verify(parser).log(eq(context), eq("EmptyWhileStatement"), eq("org/codenarc/sample/service/NewService"), eq(18), eq(""));
    verify(parser, times(2)).log(eq(context), anyString(), eq("org/codenarc/sample/service/NewService"), anyInt(), anyString());
    verify(parser, times(16)).log(eq(context), anyString(), anyString(), anyInt(), anyString());
  }

  /**
   * See http://jira.codehaus.org/browse/SONARPLUGINS-620
   */
  @Test
  public void shouldNotFailWhenLineNumberNotSpecified() {
    File fileToParse = FileUtils.toFile(getClass().getResource("CodeNarcXMLParserTest/line-number-not-specified.xml"));

    CodeNarcXMLParser parser = new CodeNarcXMLParser(null);
    parser = spy(parser);
    doNothing().when(parser).log((SensorContext) anyObject(), anyString(), anyString(), anyInt(), anyString());

    SensorContext context = mock(SensorContext.class);
    parser.parseAndLogCodeNarcResults(fileToParse, context);

    verify(parser).log(eq(context), eq("CyclomaticComplexity"), eq("org/example/Example"), eq(0),
        eq("The cyclomatic complexity for class [org.example.Example] is [27.0]"));
  }

}
