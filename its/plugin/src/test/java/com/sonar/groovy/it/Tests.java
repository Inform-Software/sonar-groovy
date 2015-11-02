/*
 * Groovy :: Integration Tests
 * Copyright (C) 2012 SonarSource
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
package com.sonar.groovy.it;

import com.google.common.collect.Iterables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  GroovyTest.class,
  MetricsTest.class,
  JaCoCo_0_7_4_Test.class,
  JaCoCo_0_7_5_Test.class,
  SurefireTest.class
})
public class Tests {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(FileLocation.of(Iterables.getOnlyElement(Arrays.asList(new File("../../sonar-groovy-plugin/target/").listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar") && !name.endsWith("-sources.jar");
      }
    }))).getAbsolutePath()))
    .addPlugin("java")
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/default.xml"))
    .build();

  public static String keyFor(String projectKey, String s) {
    return projectKey + ":src/main/groovy/" + s;
  }

  public static MavenBuild createMavenBuild() {
    return MavenBuild.create();
  }

}
