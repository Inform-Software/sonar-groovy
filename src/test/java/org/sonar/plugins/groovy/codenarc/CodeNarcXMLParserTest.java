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
import org.sonar.plugins.groovy.codenarc.CodeNarcXMLParser.CodeNarcViolation;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class CodeNarcXMLParserTest {

  @Test
  public void should_parse_report() throws Exception {
    List<CodeNarcViolation> violations = CodeNarcXMLParser.parse(FileUtils.toFile(getClass().getResource("CodeNarcXMLParserTest/sample.xml")));

    assertThat(violations.size()).isEqualTo(16);

    CodeNarcViolation violation = violations.get(0);
    assertThat(violation.getRuleName()).isEqualTo("EmptyElseBlock");
    assertThat(violation.getFilename()).isEqualTo("org/codenarc/sample/domain/SampleDomain.groovy");
    assertThat(violation.getLine()).isEqualTo(24);
    assertThat(violation.getMessage()).isEqualTo("");

    violation = violations.get(1);
    assertThat(violation.getRuleName()).isEqualTo("EmptyIfStatement");
    assertThat(violation.getFilename()).isEqualTo("org/codenarc/sample/domain/SampleDomain.groovy");
    assertThat(violation.getLine()).isEqualTo(21);
    assertThat(violation.getMessage()).isEqualTo("");
  }

  @Test
  public void should_not_fail_if_line_number_not_specified() throws Exception {
    List<CodeNarcViolation> violations = CodeNarcXMLParser.parse(FileUtils.toFile(getClass().getResource("CodeNarcXMLParserTest/line-number-not-specified.xml")));

    assertThat(violations.size()).isEqualTo(1);

    CodeNarcViolation violation = violations.get(0);
    assertThat(violation.getRuleName()).isEqualTo("CyclomaticComplexity");
    assertThat(violation.getFilename()).isEqualTo("org/example/Example.groovy");
    assertThat(violation.getLine()).isNull();
    assertThat(violation.getMessage()).isEqualTo("The cyclomatic complexity for class [org.example.Example] is [27.0]");
  }

}
