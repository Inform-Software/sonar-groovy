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
package org.sonar.plugins.groovy;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroovySensorTest {

  private Settings settings = new Settings();
  private FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
  private DefaultFileSystem fileSystem = new DefaultFileSystem(new File("."));
  private GroovySensor sensor = new GroovySensor(settings, fileLinesContextFactory, fileSystem);

  @Test
  public void should_execute_on_project() {
    fileSystem.add(new DefaultInputFile("", "fake.groovy").setLanguage(Groovy.KEY));
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void should_not_execute_if_no_groovy_files() {
    assertThat(sensor.shouldExecuteOnProject()).isFalse();
  }

  @Test
  public void compute_metrics() throws IOException {
    testMetrics(false, 5);
  }

  @Test
  public void compute_metrics_ignoring_header_comment() throws IOException {
    testMetrics(true, 3);
  }

  private void testMetrics(boolean headerComment, int expectedCommentMetric) throws IOException {
    settings.appendProperty(GroovyPlugin.IGNORE_HEADER_COMMENTS, "" + headerComment);
    File sourceDir = new File("src/test/resources/org/sonar/plugins/groovy/gmetrics");
    SensorContextTester context = SensorContextTester.create(new File(""));

    File sourceFile = new File(sourceDir, "Greeting.groovy");
    fileSystem = context.fileSystem();
    fileSystem.add(new DefaultInputDir("", sourceDir.getPath()));
    DefaultInputFile groovyFile = new DefaultInputFile("", sourceFile.getPath())
      .setLanguage(Groovy.KEY)
      .initMetadata(new String(Files.readAllBytes(sourceFile.toPath()), "UTF-8"));
    fileSystem.add(groovyFile);
    fileSystem.add(new DefaultInputFile("", "unknownFile.groovy").setLanguage(Groovy.KEY));

    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(DefaultInputFile.class))).thenReturn(fileLinesContext);

    sensor = new GroovySensor(settings, fileLinesContextFactory, fileSystem);
    sensor.execute(context);

    String key = groovyFile.key();
    assertThat(context.measure(key, CoreMetrics.FILES).value()).isEqualTo(1);
    assertThat(context.measure(key, CoreMetrics.CLASSES).value()).isEqualTo(2);
    assertThat(context.measure(key, CoreMetrics.FUNCTIONS).value()).isEqualTo(2);

    assertThat(context.measure(key, CoreMetrics.LINES).value()).isEqualTo(33);
    assertThat(context.measure(key, CoreMetrics.NCLOC).value()).isEqualTo(17);
    assertThat(context.measure(key, CoreMetrics.COMMENT_LINES).value()).isEqualTo(expectedCommentMetric);

    assertThat(context.measure(key, CoreMetrics.COMPLEXITY).value()).isEqualTo(4);
    assertThat(context.measure(key, CoreMetrics.COMPLEXITY_IN_CLASSES).value()).isEqualTo(4);
    assertThat(context.measure(key, CoreMetrics.COMPLEXITY_IN_FUNCTIONS).value()).isEqualTo(4);

    assertThat(context.measure(key, CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION).value()).isEqualTo("1=0;2=2;4=0;6=0;8=0;10=0;12=0");
    assertThat(context.measure(key, CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION).value()).isEqualTo("0=1;5=0;10=0;20=0;30=0;60=0;90=0");

    // 11 times for comment because we register comment even when ignoring header comment
    Mockito.verify(fileLinesContext, Mockito.times(11)).setIntValue(Mockito.eq(CoreMetrics.COMMENT_LINES_DATA_KEY), Matchers.anyInt(), Mockito.eq(1));
    Mockito.verify(fileLinesContext, Mockito.times(17)).setIntValue(Mockito.eq(CoreMetrics.NCLOC_DATA_KEY), Matchers.anyInt(), Mockito.eq(1));
    Mockito.verify(fileLinesContext).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 18, 1);
    Mockito.verify(fileLinesContext).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 18, 1);
    // Only "Greeting.groovy" is part of the file system.
    Mockito.verify(fileLinesContext, Mockito.times(1)).save();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("GroovySensor");
  }

}
