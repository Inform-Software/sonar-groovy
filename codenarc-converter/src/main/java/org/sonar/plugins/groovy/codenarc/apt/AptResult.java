/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2010-2020 SonarSource SA & Community
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.internal.apachecommons.lang.StringUtils;
import org.sonar.plugins.groovy.codenarc.RuleParameter;

public class AptResult {

  private static final Logger log = LoggerFactory.getLogger(AptResult.class);

  private final String rule;
  Set<RuleParameter> parameters = Sets.newHashSet();
  private StringBuilder descriptionBuilder = new StringBuilder();
  private boolean inParagraph = false;

  public AptResult(String rule) {
    this.rule = rule;
  }

  void display(int ruleFileCounter, int ruleTotalCounter, String filename) {
    log.info("==========================================");
    log.info("Rule #{} : {} ({} #{})", ruleTotalCounter, rule, filename, ruleFileCounter);
    if (descriptionBuilder.length() > 0) {
      log.info("------------------------------------------");
      log.info(getDescription());
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
    return descriptionBuilder.toString().trim();
  }

  public void appendDescription(String str) {
    descriptionBuilder.append(str);
  }

  public void appendDescriptionTrimmed(String str) {
    while (descriptionBuilder.length() > 0
        && Character.isWhitespace(descriptionBuilder.charAt(descriptionBuilder.length() - 1))) {
      descriptionBuilder.deleteCharAt(descriptionBuilder.length() - 1);
    }
    descriptionBuilder.append(str);
  }

  public void addParagraphLine(String line) {
    if (StringUtils.isBlank(line)) {
      if (inParagraph) descriptionBuilder.append("</p>\n");
      inParagraph = false;
      return;
    }

    if (!inParagraph) {
      descriptionBuilder.append("<p>");
      inParagraph = true;
    } else {
      descriptionBuilder.append(' ');
    }
    descriptionBuilder.append(line);
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }

  public void replaceDescription(AptResult other) {
    this.descriptionBuilder = new StringBuilder(other.getDescription());
  }
}
