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

import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;

public class ActiveRulesBuilderWrapper {

  private ActiveRulesBuilder builder = new ActiveRulesBuilder();
  private Object lastRule;

  boolean newType = false;
  Class<?> builderClass = null;
  private Constructor<?> ruleConstructor;
  private Method nameSetter;
  private Method internalKeySetter;
  private Method paramSetter;
  private Method activateMethod;
  private Method createMethod;
  private Method buildMethod;
  private Method addRuleMethod;

  public ActiveRulesBuilderWrapper() {
    try {
      builderClass = Class.forName("org.sonar.api.batch.rule.internal.NewActiveRule$Builder");
      newType = true;
    } catch (ClassNotFoundException e) {
      try {
        builderClass = Class.forName("org.sonar.api.batch.rule.internal.NewActiveRule");
      } catch (ClassNotFoundException e1) {
        fail("Could not initialize NewActiveRule", e1);
      }
    }
    try {
      nameSetter = builderClass.getMethod("setName", String.class);
      internalKeySetter = builderClass.getMethod("setInternalKey", String.class);
      paramSetter = builderClass.getMethod("setParam", String.class, String.class);
      if (newType) {
        ruleConstructor = builderClass.getConstructor();
        createMethod = builderClass.getMethod("setRuleKey", RuleKey.class);
        buildMethod = builderClass.getMethod("build");
        addRuleMethod = builder.getClass().getMethod("addRule", NewActiveRule.class);
      } else {
        createMethod = builder.getClass().getMethod("create", RuleKey.class);
        activateMethod = builderClass.getMethod("activate");
      }
    } catch (NoSuchMethodException | SecurityException e) {
      fail("Could not look up a method", e);
    }
  }

  public ActiveRulesBuilderWrapper addRule(String key) {
    addLastRule();
    RuleKey ruleKey = RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY, key);
    try {
      if (newType) {
        lastRule = ruleConstructor.newInstance();
        createMethod.invoke(lastRule, ruleKey);
      } else {
        lastRule = createMethod.invoke(builder, ruleKey);
      }
    } catch (IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | InstantiationException e) {
      fail("Could not create new rule builder.", e);
    }
    setInternalKey(key);
    return this;
  }

  public ActiveRulesBuilderWrapper setName(String name) {
    try {
      nameSetter.invoke(lastRule, name);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail("Could not execute 'setName'", e);
    }
    return this;
  }

  public ActiveRulesBuilderWrapper setInternalKey(String key) {
    try {
      internalKeySetter.invoke(lastRule, key);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail("Could not execute 'setInternalKey'", e);
    }
    return this;
  }

  public ActiveRulesBuilderWrapper addParam(String key, String value) {
    try {
      paramSetter.invoke(lastRule, key, value);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail("Could not execute 'setParam'", e);
    }
    return this;
  }

  private void addLastRule() {
    if (lastRule != null) {
      try {
        if (newType) {
          addRuleMethod.invoke(builder, buildMethod.invoke(lastRule));
        } else {
          activateMethod.invoke(lastRule);
        }
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        fail("Could not add rule to active rules.", e);
      }
      lastRule = null;
    }
  }

  public ActiveRules build() {
    addLastRule();
    return builder.build();
  }
}
