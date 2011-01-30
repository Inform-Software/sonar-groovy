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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class CodeNarcXMLParserTest {

  @Test
  public void testCodeNarcReportParser() {
    File fileToParse = FileUtils.toFile(getClass().getResource("/org/sonar/plugins/groovy/CodeNarcXmlSampleReport.xml"));

    CodeNarcXMLParser parser = new CodeNarcXMLParser(null, null);
    parser = spy(parser);
    doNothing().when(parser).log(anyString(), anyString(), anyInt(), anyString());

    parser.parseAndLogCodeNarcResults(fileToParse);

    verify(parser).log("EmptyIfStatement", "org/codenarc/sample/domain/SampleDomain", 21, "");
    verify(parser).log("EmptyWhileStatement", "org/codenarc/sample/service/NewService", 18, "");
    verify(parser, times(2)).log(anyString(), eq("org/codenarc/sample/service/NewService"), anyInt(), anyString());
    verify(parser, times(16)).log(anyString(), anyString(), anyInt(), anyString());
  }

}
