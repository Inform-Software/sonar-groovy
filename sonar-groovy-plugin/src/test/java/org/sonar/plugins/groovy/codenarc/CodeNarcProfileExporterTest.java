/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2019 SonarSource SA & Community
 * mailto:info AT sonarsource DOT com
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
package org.sonar.plugins.groovy.codenarc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.commons.lang.CharUtils;
import org.assertj.core.api.Fail;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.rule.internal.NewActiveRule.Builder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.groovy.TestUtils;

public class CodeNarcProfileExporterTest {

  private StringWriter writer;
  private CodeNarcProfileExporter exporter;

  @Before
  public void setUp() {
    writer = new StringWriter();
    exporter = new CodeNarcProfileExporter(writer);
  }

  @Test
  public void shouldExportProfile() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
        .addRule(
            createNewActiveRuleForTest(
                "org.codenarc.rule.basic.AddEmptyStringRule",
                "Add Empty String"
            )
        )
        .addRule(
            createNewActiveRuleForTest(
                "org.codenarc.rule.size.ClassSizeRule",
                "Class Size"
            )
        )
        .build();
    exporter.exportProfile(activeRules);

    assertSimilarXml(
        TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportProfile.xml"),
        writer.toString());
  }

  @Test
  public void shouldFailToExport() throws IOException {
    Writer writer = Mockito.mock(Writer.class);
    Mockito.when(writer.append(Matchers.any(CharSequence.class))).thenThrow(new IOException());
    exporter = new CodeNarcProfileExporter(writer);

    try {
      exporter.exportProfile(new ActiveRulesBuilder().build());
      Fail.fail("Should have failed");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("Fail to export CodeNarc profile");
    }
  }

  @Test
  public void shouldExportParameters() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
        .addRule(
            createNewActiveRuleForTestWithParam(
                "org.codenarc.rule.size.ClassSizeRule",
                "Class Size",
                "maxLines",
                "20"
            )
        )
        .build();

    exporter.exportProfile(activeRules);

    assertSimilarXml(
        TestUtils
            .getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportParameters.xml"),
        writer.toString());
  }

  @Test
  public void shouldNotExportUnsetParameters() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
        .addRule(
            createNewActiveRuleForTestWithParam(
                "org.codenarc.rule.size.ClassSizeRule",
                "Class Size",
                "maxLines",
                null
            )
        )
        .build();

    exporter.exportProfile(activeRules);

    assertSimilarXml(
        TestUtils.getResource(
            "/org/sonar/plugins/groovy/codenarc/exportProfile/exportNullParameters.xml"),
        writer.toString());
  }

  @Test
  public void shouldExportFixedRulesCorrectly() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
        .addRule(
            createNewActiveRuleForTest(
                "org.codenarc.rule.design.PrivateFieldCouldBeFinalRule.fixed",
                "Private Field Could Be Final"
            )
        )
        .build();

    exporter.exportProfile(activeRules);

    assertSimilarXml(
        TestUtils
            .getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportFixedRules.xml"),
        writer.toString());
  }

  @Test
  @Ignore("Is this rule still pertinent, as the rule parameters are kept server-side? I assume defaults will be brought down as params")
  public void shouldNotExportParametersWithDefaultValue() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
        .addRule(
            createNewActiveRuleForTestWithParam(
                "org.codenarc.rule.size.ClassSizeRule",
                "Class Size",
                "maxLines",
                "20"
            )
        )
        .build();

    exporter.exportProfile(activeRules);

    assertSimilarXml(
        TestUtils.getResource(
            "/org/sonar/plugins/groovy/codenarc/exportProfile/exportNullParameters.xml"),
        writer.toString());
  }

  @Test
  public void shouldEscapeExportedParameters() throws Exception {
    ActiveRules activeRules = new ActiveRulesBuilder()
        .addRule(
            createNewActiveRuleForTestWithParam(
                "org.codenarc.rule.naming.ClassNameRule",
                "Class Name",
                "regex",
                "[A-Z]+[a-z&&[^bc]]"
            )
        )
        .build();

    exporter.exportProfile(activeRules);

    assertSimilarXml(
        TestUtils.getResource(
            "/org/sonar/plugins/groovy/codenarc/exportProfile/exportEscapedParameters.xml"),
        writer.toString());
  }

  private void assertSimilarXml(File expectedFile, String xml) throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    Reader reader = new FileReader(expectedFile);
    Diff diff = XMLUnit.compareXML(reader, xml);
    String message = "Diff: " + diff + CharUtils.LF + "XML: " + xml;
    Assert.assertTrue(message, diff.similar());
  }

  private NewActiveRule createNewActiveRuleForTest(String ruleKey, String ruleName) {
    return createNewActiveRuleBuilderForTest(ruleKey, ruleName).build();
  }

  private NewActiveRule createNewActiveRuleForTestWithParam(
      String ruleKey,
      String ruleName,
      String paramName,
      String paramValue
  ) {
    return createNewActiveRuleBuilderForTest(ruleKey, ruleName)
        .setParam(paramName, paramValue)
        .build();
  }

  private Builder createNewActiveRuleBuilderForTest(String ruleKey, String ruleName) {
    return new Builder()
        .setRuleKey(
            RuleKey.of(
                CodeNarcRulesDefinition.REPOSITORY_KEY,
                ruleKey
            )
        )
        .setName(ruleName)
        .setSeverity(Severity.MAJOR);
  }
}
