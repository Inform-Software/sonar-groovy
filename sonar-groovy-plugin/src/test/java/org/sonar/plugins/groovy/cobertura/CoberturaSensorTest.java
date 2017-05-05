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
package org.sonar.plugins.groovy.cobertura;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.sonar.plugins.groovy.GroovyPlugin;
import org.sonar.plugins.groovy.foundation.Groovy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaSensorTest {

  private Settings settings;
  private CoberturaSensor sensor;
  private DefaultFileSystem fileSystem;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    settings.setProperty(GroovyPlugin.COBERTURA_REPORT_PATH, "src/test/resources/org/sonar/plugins/groovy/cobertura/coverage.xml");
    fileSystem = new DefaultFileSystem(new File("."));
    sensor = new CoberturaSensor(settings, fileSystem);
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Groovy.KEY);
  }

  /**
   * See SONARPLUGINS-696
   */
  @Test
  public void should_parse_report() {
    FilePredicates fp = mock(FilePredicates.class);

    class CustomFilePredicate implements FilePredicate {

      final String fileName;

      CustomFilePredicate(String fileName) {
        this.fileName = fileName;
      }

      @Override
      public boolean apply(InputFile inputFile) {
        return true;
      }
    }

    when(fp.hasAbsolutePath(ArgumentMatchers.anyString())).thenAnswer(new Answer<FilePredicate>() {
      @Override
      public FilePredicate answer(InvocationOnMock invocation) throws Throwable {
        return new CustomFilePredicate(invocation.<String>getArgument(0));
      }
    });

    FileSystem mockfileSystem = mock(FileSystem.class);
    when(mockfileSystem.predicates()).thenReturn(fp);
    when(mockfileSystem.hasFiles(ArgumentMatchers.nullable(FilePredicate.class))).thenReturn(true);

    Map<String, DefaultInputFile> groovyFilesByName = new HashMap<>();

    when(mockfileSystem.inputFile(any(FilePredicate.class))).thenAnswer(new Answer<InputFile>() {
      boolean firstCall = true;

      @Override
      public InputFile answer(InvocationOnMock invocation) throws Throwable {
        if (firstCall) {
          // The first class in the test coverage.xml is a java class and the rest are groovy
          firstCall = false;
          return new DefaultInputFile("", "fake.java").setLanguage("java");
        }
        String fileName = invocation.<CustomFilePredicate>getArgument(0).fileName;
        DefaultInputFile groovyFile;
        if (!groovyFilesByName.containsKey(fileName)) {
          // store groovy file as default input files
          groovyFile = new DefaultInputFile("", fileName).setLanguage(Groovy.KEY).setType(Type.MAIN).setLines(Integer.MAX_VALUE);
          groovyFilesByName.put(fileName, groovyFile);
        }
        return groovyFilesByName.get(fileName);
      }
    });
    sensor = new CoberturaSensor(settings, mockfileSystem);

    SensorContextTester context = SensorContextTester.create(new File(""));
    sensor.execute(context);

    // random pick groovy file
    String filekey = ":/Users/cpicat/myproject/grails-app/domain/AboveEighteenFilters.groovy";
    int[] lineHits = {2, 6, 7};
    int[] lineNoHits = {9, 10, 11};

    for (int line : lineHits) {
      assertThat(context.lineHits(filekey, CoverageType.UNIT, line)).isEqualTo(1);
    }
    for (int line : lineNoHits) {
      assertThat(context.lineHits(filekey, CoverageType.UNIT, line)).isEqualTo(0);
    }

    // No value for java file
    assertThat(context.lineHits(":/Users/cpicat/myproject/grails-app/domain/com/test/web/EmptyResultException.java", CoverageType.UNIT, 16)).isNull();
  }

  @Test
  public void should_not_save_any_measure_if_files_can_not_be_found() {
    FileSystem mockfileSystem = mock(FileSystem.class);
    when(mockfileSystem.predicates()).thenReturn(fileSystem.predicates());
    when(mockfileSystem.inputFile(any(FilePredicate.class))).thenReturn(null);
    sensor = new CoberturaSensor(settings, mockfileSystem);

    SensorContext context = mock(SensorContext.class);
    sensor.execute(context);

    Mockito.verify(context, Mockito.never()).newCoverage();
  }

  @Test
  public void should_not_parse_report_if_settings_does_not_contain_report_path() {
    DefaultFileSystem fileSystem = new DefaultFileSystem(new File("."));
    fileSystem.add(new DefaultInputFile("", "fake.groovy").setLanguage(Groovy.KEY));
    sensor = new CoberturaSensor(new Settings(), fileSystem);

    SensorContext context = mock(SensorContext.class);
    sensor.execute(context);

    Mockito.verify(context, Mockito.never()).newCoverage();
  }

  @Test
  public void should_not_parse_report_if_report_does_not_exist() {
    Settings settings = new Settings();
    settings.setProperty(GroovyPlugin.COBERTURA_REPORT_PATH, "org/sonar/plugins/groovy/cobertura/fake-coverage.xml");

    DefaultFileSystem fileSystem = new DefaultFileSystem(new File("."));
    fileSystem.add(new DefaultInputFile("", "fake.groovy").setLanguage(Groovy.KEY));

    sensor = new CoberturaSensor(settings, fileSystem);

    SensorContext context = mock(SensorContext.class);
    sensor.execute(context);

    Mockito.verify(context, Mockito.never()).newCoverage();
  }

  @Test
  public void should_use_relative_path_to_get_report() {
    Settings settings = new Settings();
    settings.setProperty(GroovyPlugin.COBERTURA_REPORT_PATH, "//org/sonar/plugins/groovy/cobertura/fake-coverage.xml");

    DefaultFileSystem fileSystem = new DefaultFileSystem(new File("."));
    fileSystem.add(new DefaultInputFile("", "fake.groovy").setLanguage(Groovy.KEY));

    sensor = new CoberturaSensor(settings, fileSystem);

    SensorContext context = mock(SensorContext.class);
    sensor.execute(context);

    Mockito.verify(context, Mockito.never()).newCoverage();
  }

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
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("Groovy CoberturaSensor");
  }

}
