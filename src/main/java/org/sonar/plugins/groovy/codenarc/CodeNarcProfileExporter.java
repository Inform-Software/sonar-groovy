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

import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.groovy.GroovyConstants;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.IOException;
import java.io.Writer;

public class CodeNarcProfileExporter extends ProfileExporter {

  public CodeNarcProfileExporter() {
    super(GroovyConstants.REPOSITORY_KEY, Groovy.KEY); // TODO
  }

  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    try {
      writer.append("<ruleset xmlns=\"http://codenarc.org/ruleset/1.0\"\n" +
          "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
          "          xsi:schemaLocation=\"http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd\"\n" +
          "          xsi:noNamespaceSchemaLocation=\"http://codenarc.org/ruleset-schema.xsd\">\n");
      for (ActiveRule activeRule : profile.getActiveRules()) {
        if (GroovyConstants.REPOSITORY_KEY.equals(activeRule.getRepositoryKey())) {
          writer.append("<rule class=\"")
              .append(activeRule.getRuleKey()); // TODO configKey?
          if (activeRule.getActiveRuleParams().isEmpty()) {
            writer.append("\"/>\n");
          } else {
            writer.append("\">\n");
            for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
              writer.append("<property name=\"")
                  .append(activeRuleParam.getKey())
                  .append("\" value=\"")
                  .append(activeRuleParam.getValue())
                  .append("\"/>\n");
            }
            writer.append("</rule>\n");
          }
        }
      }
      writer.append("</ruleset>");
    } catch (IOException e) {
      throw new SonarException("Fail to export CodeNarc profile : " + profile, e);
    }
  }

}
