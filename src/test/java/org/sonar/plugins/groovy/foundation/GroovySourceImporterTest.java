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

package org.sonar.plugins.groovy.foundation;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Evgeny Mandrikov
 */
public class GroovySourceImporterTest {

  private GroovySourceImporter importer;

  @Before
  public void setUp() throws Exception {
    importer = new GroovySourceImporter(Groovy.INSTANCE);
  }

  @Test
  public void shouldCreateResource() {
    Resource resource = importer.createResource(new File(newDir("source1"), "/Utils.groovy"), Arrays.asList(newDir("source1")), false);
    assertThat(resource).isInstanceOf(GroovyFile.class);
    assertThat(ResourceUtils.isUnitTestClass(resource)).isFalse();
    assertThat(resource.getKey()).isEqualTo(GroovyPackage.DEFAULT_PACKAGE_NAME + ".Utils");
    assertThat(resource.getName()).isEqualTo("Utils");
  }

  @Test
  public void shouldCreateTestResource() {
    Resource resource = importer.createResource(new File(newDir("tests"), "UtilsTest.groovy"), Arrays.asList(newDir("tests")), true);
    assertThat(resource).isInstanceOf(GroovyFile.class);
    assertThat(ResourceUtils.isUnitTestClass(resource)).isTrue();
  }

  @Test
  public void should_accept_null() {
    assertThat(importer.createResource(null, Arrays.asList(newDir("source1")), false)).isNull();
  }

  @Test
  public void test_toString() {
    assertThat(importer.toString()).isEqualTo("GroovySourceImporter");
  }

  private File newDir(String relativePath) {
    return new File("target", relativePath);
  }

}
