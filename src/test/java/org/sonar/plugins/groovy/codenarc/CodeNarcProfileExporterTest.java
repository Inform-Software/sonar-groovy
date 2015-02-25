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

import org.apache.commons.lang.CharUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringWriter;

public class CodeNarcProfileExporterTest {

  private StringWriter writer;
  private CodeNarcProfileExporter exporter;
  private RulesProfile profile;

  @Before
  public void setUp() {
    writer = new StringWriter();
    exporter = new CodeNarcProfileExporter(writer);
    profile = RulesProfile.create("Sonar Groovy way", Groovy.KEY);
  }

  @Test
  public void shouldExportProfile() throws Exception {
    Rule rule = Rule.create(CodeNarcRulesDefinition.REPOSITORY_KEY, "org.codenarc.rule.basic.AddEmptyStringRule", "Add Empty String");
    profile.activateRule(rule, RulePriority.MAJOR);
    rule = Rule.create(CodeNarcRulesDefinition.REPOSITORY_KEY, "org.codenarc.rule.size.ClassSizeRule", "Class Size");
    profile.activateRule(rule, RulePriority.MAJOR);
    exporter.exportProfile(profile);

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportProfile.xml"),
      writer.toString());

  }

  @Test
  public void shouldNotExportParameters() throws Exception {
    Rule rule = Rule.create(CodeNarcRulesDefinition.REPOSITORY_KEY, "org.codenarc.rule.size.ClassSizeRule", "Class Size");
    rule.createParameter("maxLines");
    profile.activateRule(rule, RulePriority.MAJOR).setParameter("maxLines", "20");

    exporter.exportProfile(profile);

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportParameters.xml"),
      writer.toString());
  }

  @Test
  public void shouldNotExportUnsetParameters() throws Exception {
    Rule rule = Rule.create(CodeNarcRulesDefinition.REPOSITORY_KEY, "org.codenarc.rule.size.ClassSizeRule", "Class Size");
    rule.createParameter("maxLines");
    profile.activateRule(rule, RulePriority.MAJOR).setParameter("maxLines", null);

    exporter.exportProfile(profile);

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportNullParameters.xml"),
      writer.toString());
  }

  private void assertSimilarXml(File expectedFile, String xml) throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    Reader reader = new FileReader(expectedFile);
    Diff diff = XMLUnit.compareXML(reader, xml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    Assert.assertTrue(message, diff.similar());
  }
}
