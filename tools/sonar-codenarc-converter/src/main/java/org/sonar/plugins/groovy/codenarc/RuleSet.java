/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2011-2016 SonarSource SA
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

import org.codenarc.rule.AbstractRule;

public enum RuleSet {
  BASIC("basic"),
  // new rule set in 0.14
  SERIALIZATION("serialization"),
  BRACES("braces"),
  CONCURRENCY("concurrency"),
  DESIGN("design"),
  DRY("dry"),
  EXCEPTIONS("exceptions"),
  GENERIC("generic"),
  GRAILS("grails"),
  IMPORTS("imports"),
  JUNIT("junit"),
  LOGGING("logging"),
  NAMING("naming"),
  SIZE("size"),
  UNNECESSARY("unnecessary"),
  UNUSED("unused"),
  // new rule set in 0.14
  JDBC("jdbc"),
  // new rule set in 0.14
  SECURITY("security"),
  // new rule set in 0.15
  FORMATTING("formatting"),
  // new rule set in 0.16
  CONVENTION("convention"),
  // new rule set in 0.16
  GROOVYISM("groovyism");

  private final String label;

  RuleSet(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static RuleSet getCategory(Class<? extends AbstractRule> ruleClass) {
    String[] name = ruleClass.getCanonicalName().split("\\.");
    return RuleSet.valueOf(name[3].toUpperCase());
  }
}