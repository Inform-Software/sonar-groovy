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

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarWayProfileTest {

  private SonarWayProfile profileDefinition;
  private ValidationMessages messages;

  @Before
  public void setUp() {
    messages = ValidationMessages.create();
    RuleFinder ruleFinder = createRuleFinder();
    profileDefinition = new SonarWayProfile(ruleFinder);
  }

  @Test
  public void test() {
    RulesProfile profile = profileDefinition.createProfile(messages);

    assertThat(profile.getActiveRules().size(), is(32));
  }

  private RuleFinder createRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find((RuleQuery) anyObject())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) throws Throwable {
        RuleQuery query = (RuleQuery) invocation.getArguments()[0];
        return Rule.create(query.getRepositoryKey(), query.getConfigKey(), "Rule name - " + query.getConfigKey());
      }
    });
    return ruleFinder;
  }
}
