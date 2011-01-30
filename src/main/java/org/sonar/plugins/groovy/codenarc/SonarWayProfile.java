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

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.groovy.GroovyConstants;
import org.sonar.plugins.groovy.foundation.Groovy;

public class SonarWayProfile extends ProfileDefinition {

  private RuleFinder ruleFinder;

  public SonarWayProfile(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages validation) {
    RulesProfile profile = RulesProfile.create();
    profile.setLanguage(Groovy.KEY);
    profile.setName("Sonar Groovy way");

    activateRule(validation, profile, "org.codenarc.rule.exceptions.CatchErrorRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.CatchExceptionRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.CatchNullPointerExceptionRule", RulePriority.CRITICAL);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.CatchRuntimeExceptionRule", RulePriority.CRITICAL);
    activateRule(validation, profile, "org.codenarc.rule.basic.CloneableWithoutCloneRule", RulePriority.MINOR);
    activateRule(validation, profile, "org.codenarc.rule.braces.ElseBlockBracesRule", RulePriority.MINOR);
    activateRule(validation, profile, "org.codenarc.rule.grails.GrailsPublicControllerMethodRule", RulePriority.MINOR);
    activateRule(validation, profile, "org.codenarc.rule.grails.GrailsServletContextReferenceRule", RulePriority.MINOR);
    activateRule(validation, profile, "org.codenarc.rule.grails.GrailsSessionReferenceRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.grails.GrailsStatelessServiceRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.size.NestedBlockDepthRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.concurrency.NestedSynchronizationRule", RulePriority.CRITICAL);
    activateRule(validation, profile, "org.codenarc.rule.logging.PrintStackTraceRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.logging.PrintlnRule", RulePriority.MINOR);
    activateRule(validation, profile, "org.codenarc.rule.basic.ReturnFromFinallyBlockRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.concurrency.SynchronizedMethodRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.logging.SystemErrPrintRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.logging.SystemOutPrintRule", RulePriority.INFO);
    activateRule(validation, profile, "org.codenarc.rule.concurrency.SystemRunFinalizersOnExitRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.concurrency.ThreadYieldRule", RulePriority.CRITICAL);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.ThrowErrorRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.ThrowExceptionRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.basic.ThrowExceptionFromFinallyBlockRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.ThrowNullPointerExceptionRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.ThrowRuntimeExceptionRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.exceptions.ThrowThrowableRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.imports.UnnecessaryGroovyImportRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.imports.UnusedImportRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.unused.UnusedPrivateFieldRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.unused.UnusedPrivateMethodRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.unused.UnusedVariableRule", RulePriority.MAJOR);
    activateRule(validation, profile, "org.codenarc.rule.concurrency.VolatileLongOrDoubleFieldRule", RulePriority.MAJOR);

    return profile;
  }

  private void activateRule(ValidationMessages validation, RulesProfile profile, String ruleClass, RulePriority priority) {
    Rule rule = getRule(ruleClass);
    if (rule != null) {
      profile.activateRule(rule, priority);
    } else {
      validation.addWarningText("Unknwon rule : " + ruleClass);
    }
  }

  private Rule getRule(String key) {
    return ruleFinder.find(RuleQuery.create().withRepositoryKey(GroovyConstants.REPOSITORY_KEY).withKey(key));
  }

}
