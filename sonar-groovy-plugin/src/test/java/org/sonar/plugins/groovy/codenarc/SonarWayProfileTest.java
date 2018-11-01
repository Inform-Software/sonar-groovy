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
package org.sonar.plugins.groovy.codenarc;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.groovy.foundation.Groovy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarWayProfileTest {

  @Test
  public void shouldCreateProfile() {
    ProfileDefinition profileDefinition = new SonarWayProfile(new XMLProfileParser(newRuleFinder()));
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = profileDefinition.createProfile(messages);

    assertThat(profile.getName()).isEqualTo("Sonar way");
    assertThat(profile.getLanguage()).isEqualTo(Groovy.KEY);
    assertThat(profile.getActiveRules()).hasSize(351);
    assertThat(messages.hasErrors()).isFalse();

    CodeNarcRulesDefinition definition = new CodeNarcRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(CodeNarcRulesDefinition.REPOSITORY_KEY);

    Map<String, RulesDefinition.Rule> rules = new HashMap<>();
    for (RulesDefinition.Rule rule : repository.rules()) {
      rules.put(rule.key(), rule);
    }
    for (ActiveRule activeRule : profile.getActiveRules()) {
      assertThat(rules.containsKey(activeRule.getConfigKey())).as("No such rule: " + activeRule.getConfigKey()).isTrue();
    }
  }

  private RuleFinder newRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock iom) throws Throwable {
        return Rule.create((String) iom.getArguments()[0], (String) iom.getArguments()[1], (String) iom.getArguments()[1]);
      }
    });
    return ruleFinder;
  }
}
