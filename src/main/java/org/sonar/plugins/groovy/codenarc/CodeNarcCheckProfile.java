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

import org.apache.commons.io.FileUtils;
import org.sonar.api.checks.profiles.Check;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CodeNarcCheckProfile {
  public String export(CheckProfile profile) {
    StringBuilder sb = new StringBuilder();
    appendHeader(sb, profile);
    appendChecks(sb, profile);
    appendFooter(sb);
    return sb.toString();
  }

  private void appendChecks(StringBuilder sb, CheckProfile profile) {
    List<Check> checks = profile.getChecks(Groovy.KEY);
    if (checks != null) {
      for (Check check : checks) {
        sb.append("<rule class=\"");
        sb.append(check.getTemplateKey());
        sb.append("\"/>\n");
      }
    }
  }

  private void appendFooter(StringBuilder sb) {
    sb.append("</ruleset>");
  }

  private void appendHeader(StringBuilder sb, CheckProfile profile) {
    sb.append("<ruleset xmlns=\"http://codenarc.org/ruleset/1.0\"\n" +
      "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "          xsi:schemaLocation=\"http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd\"\n" +
      "          xsi:noNamespaceSchemaLocation=\"http://codenarc.org/ruleset-schema.xsd\">\n");
  }

  public void save(CheckProfile profile, File file) throws IOException {
    FileUtils.writeStringToFile(file, export(profile));
  }
}
