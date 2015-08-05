/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.groovy.codenarc;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class CodeNarcRulesDefinitionTest {

  @Test
  public void test() {
    CodeNarcRulesDefinition definition = new CodeNarcRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(CodeNarcRulesDefinition.REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo(CodeNarcRulesDefinition.REPOSITORY_NAME);
    assertThat(repository.language()).isEqualTo(Groovy.KEY);

    List<Rule> rules = repository.rules();
    assertThat(rules).hasSize(345);

    List<String> missingDebt = Lists.newLinkedList();
    for (Rule rule : rules) {
      assertThat(rule.key()).isNotNull();
      assertThat(rule.internalKey()).isNotNull();
      assertThat(rule.name()).isNotNull();
      assertThat(rule.htmlDescription()).isNotNull();
      if (rule.debtRemediationFunction() == null) {
        missingDebt.add(rule.key());
      }
    }
    // From SONARGROOV-36, 'org.codenarc.rule.generic.IllegalSubclassRule' does not have SQALE mapping by purpose
    assertThat(missingDebt).containsOnly("org.codenarc.rule.generic.IllegalSubclassRule.fixed");

    Rule rule = repository.rule("org.codenarc.rule.braces.ElseBlockBracesRule");
    assertThat(rule.params()).hasSize(1);
    assertThat(rule.params().get(0).defaultValue()).isEqualToIgnoringCase("false");
  }
}
