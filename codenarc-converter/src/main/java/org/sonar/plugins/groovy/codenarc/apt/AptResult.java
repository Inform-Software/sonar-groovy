/*
 * Sonar CodeNarc Converter
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
package org.sonar.plugins.groovy.codenarc.apt;

import com.google.common.collect.Sets;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.groovy.codenarc.RuleParameter;

public class AptResult {

  private static final Logger log = LoggerFactory.getLogger(AptResult.class);

  String rule;
  Set<RuleParameter> parameters = Sets.newHashSet();
  String description = "";

  public AptResult(String rule) {
    this.rule = rule;
  }

  void display(int ruleFileCounter, int ruleTotalCounter, String filename) {
    log.info("==========================================");
    log.info("Rule #{} : {} ({} #{})", ruleTotalCounter, rule, filename, ruleFileCounter);
    if (StringUtils.isNotBlank(description)) {
      log.info("------------------------------------------");
      log.info(description);
    }
    if (!parameters.isEmpty()) {
      log.info("------------------------------------------");
      log.info("Parameters: ");
      for (RuleParameter parameter : parameters) {
        log.info("  * \"{}\"", parameter.key());
        log.info("    - defaultValue: {}", parameter.defaultValue());
        log.info("    - description: {}", parameter.description());
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
    return description.trim();
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }
}
