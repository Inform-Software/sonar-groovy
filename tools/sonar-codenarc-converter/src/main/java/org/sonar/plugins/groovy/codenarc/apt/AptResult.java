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
package org.sonar.plugins.groovy.codenarc.apt;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.groovy.codenarc.RuleParameter;

import java.util.Set;

public class AptResult {
  String rule;
  Set<RuleParameter> parameters = Sets.newHashSet();
  String description = "";

  public AptResult(String rule) {
    this.rule = rule;
  }

  void display(int ruleFileCounter, int ruleTotalCounter, String filename) {
    System.out.println("==========================================");
    System.out.println("Rule #" + ruleTotalCounter + " : " + rule + " (" + filename + " #" + ruleFileCounter
      + ")");
    if (StringUtils.isNotBlank(description)) {
      System.out.println("------------------------------------------");
      System.out.println(description);
    }
    if (!parameters.isEmpty()) {
      System.out.println("------------------------------------------");
      System.out.println("Parameters: ");
      for (RuleParameter parameter : parameters) {
        System.out.println("  * \"" + parameter.key + "\"");
        System.out.println("    - defaultValue: "
          + (parameter.defaultValue == null ? "" : parameter.defaultValue));
        System.out.println("    - description: " + parameter.description);
      }
    }
  }

  public String getRule() {
    return rule;
  }

  public Set<RuleParameter> getParameters() {
    return parameters;
  }

  public String getDescription() {
    return description;
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }
}
