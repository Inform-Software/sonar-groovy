/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.groovy.codenarc;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class CodeNarcXMLParserTest {

  @Test
  public void testCodeNarcReportParser() {
    GroovyMessageDispatcher messageDispatcher = mock(GroovyMessageDispatcher.class);
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);

    when(project.getFileSystem()).thenReturn(fileSystem);
    List<java.io.File> l = new ArrayList<java.io.File>();
    l.add(new java.io.File(""));
    when(fileSystem.getSourceDirs()).thenReturn(l);

    File fileToParse = FileUtils.toFile(getClass().getResource("/org/sonar/plugins/groovy/CodeNarcXmlSampleReport.xml"));
    new CodeNarcXMLParser(messageDispatcher).parseAndLogCodeNarcResults(fileToParse);

    verify(messageDispatcher).log("EmptyIfStatement", "org/codenarc/sample/domain/SampleDomain", 21, "");
    verify(messageDispatcher).log("EmptyWhileStatement", "org/codenarc/sample/service/NewService", 18, "");
    verify(messageDispatcher, times(2)).log(anyString(), eq("org/codenarc/sample/service/NewService"), anyInt(), anyString());
    verify(messageDispatcher, times(16)).log(anyString(), anyString(), anyInt(), anyString());
  }

}
