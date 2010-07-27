/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.plugins.groovy.gmetrics;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.IsMeasure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
public class GMetricsXMLParserTest {

  @Ignore
  public void testGMetricsReportParser() {
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    CheckProfile checkProfile = mock(CheckProfile.class);

    when(project.getFileSystem()).thenReturn(fileSystem);
    List<File> l = new ArrayList<File>();
    l.add(new File(""));
    when(fileSystem.getSourceDirs()).thenReturn(l);

    File fileToParse = FileUtils.toFile(getClass().getResource("/org/sonar/plugins/groovy/GMetricsSampleReport.xml"));
    new GMetricsXMLParser().parseAndProcessGMetricsResults(fileToParse, context);


    org.sonar.api.resources.File file = new org.sonar.api.resources.File("org.gmetrics.analyzer.FilesystemSourceAnalyzer");
    verify(context).saveMeasure(eq(file), eq(CoreMetrics.FUNCTIONS), eq(7.0));
    verify(context).saveMeasure(eq(file), eq(CoreMetrics.COMPLEXITY), eq(13.0));
    verify(context).saveMeasure(eq(new org.sonar.api.resources.File("org.gmetrics.analyzer.FilesystemSourceAnalyzer")), argThat(
      new IsMeasure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=4;2=2;4=1;6=0;8=0;10=0;12=0")));
    verify(context).saveMeasure(eq(new org.sonar.api.resources.File("org.gmetrics.analyzer.FilesystemSourceAnalyzer")), argThat(
      new IsMeasure(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, "0=0;5=0;10=1;20=0;30=0;60=0;90=0")));
  }
}
