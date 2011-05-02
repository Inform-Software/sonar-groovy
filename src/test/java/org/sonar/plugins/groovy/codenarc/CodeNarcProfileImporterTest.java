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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.test.TestUtils;

import java.io.Reader;
import java.io.StringReader;

public class CodeNarcProfileImporterTest {

  private CodeNarcProfileImporter importer;
  private ValidationMessages messages;

  @Before
  public void setUp() {
    importer = new CodeNarcProfileImporter(newRuleFinder());
    messages = ValidationMessages.create();
  }

  @Test
  public void shouldImportProfile() {
    Reader reader = new StringReader(
        TestUtils.getResourceContent("/org/sonar/plugins/groovy/codenarc/CodeNarcProfileExporterTest/exportProfile.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(messages.hasErrors(), is(false));
    assertThat(profile.getActiveRules().size(), is(2));
    assertThat(profile.getActiveRule(CodeNarcConstants.REPOSITORY_KEY, "org.codenarc.rule.basic.AddEmptyStringRule"), notNullValue());
    assertThat(profile.getActiveRule(CodeNarcConstants.REPOSITORY_KEY, "org.codenarc.rule.size.ClassSizeRule"), notNullValue());
  }

  @Test
  public void shouldImportParameters() {
    Reader reader = new StringReader(
        TestUtils.getResourceContent("/org/sonar/plugins/groovy/codenarc/CodeNarcProfileExporterTest/exportParameters.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(messages.hasErrors(), is(false));
    ActiveRule activeRule = profile.getActiveRule(CodeNarcConstants.REPOSITORY_KEY, "org.codenarc.rule.size.ClassSizeRule");
    assertThat(activeRule.getActiveRuleParams().size(), is(1));
    assertThat(activeRule.getParameter("maxLines"), is("20"));
  }

  private RuleFinder newRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) throws Throwable {
        String repositoryKey = (String) invocation.getArguments()[0];
        String ruleKey = (String) invocation.getArguments()[1];
        Rule rule = Rule.create().setRepositoryKey(repositoryKey).setKey(ruleKey);
        if (StringUtils.equals(ruleKey, "org.codenarc.rule.size.ClassSizeRule")) {
          rule.createParameter("maxLines");
        }
        return rule;
      }
    });
    return ruleFinder;
  }
}
