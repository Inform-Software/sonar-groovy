/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.groovy.codenarc;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.checkers.MessageDispatcher;
import org.sonar.api.checks.profiles.Check;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.api.checks.templates.CheckTemplate;
import org.sonar.api.checks.templates.CheckTemplateRepository;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Message;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFile;

import java.util.List;
import java.util.Locale;

public class GroovyMessageDispatcher {

  private CheckProfile profile;
  private Project project;
  private MessageDispatcher messageDispatcher;
  private Groovy groovy;
  private CheckTemplateRepository repo;

  public GroovyMessageDispatcher(CheckProfile profile, Project project, Groovy groovy, SensorContext context, CheckTemplateRepository repo) {
    this.profile = profile;
    this.project = project;
    this.groovy = groovy;
    messageDispatcher = new MessageDispatcher(context);
    messageDispatcher.registerCheckers(profile);
    this.repo = repo;
  }

  protected void log(String checkKey, String filename, Integer line, String message) {
    Check check = profile.getCheck(Groovy.KEY, mapConfigKeyToKey(checkKey));
    if (check != null) {
      GroovyFile sonarFile = new GroovyFile(StringUtils.replace(filename, "/", "."));

      messageDispatcher.log(sonarFile, new GroovyMessage(check, line, message));
    }
  }

  private String mapConfigKeyToKey(String configKey) {
    List<CheckTemplate> checkTemplates = repo.getTemplates();
    for (CheckTemplate checkTemplate : checkTemplates) {
      if(configKey.equals(checkTemplate.getConfigKey())) {
        return checkTemplate.getKey();
      }
    }
    throw new SonarException("Check " + configKey + " not found");
  }

  static class GroovyMessage implements Message {

    private Check check;
    private Integer line;
    private String text;

    GroovyMessage(Check check, Integer line, String text) {
      this.check = check;
      this.line = line;
      this.text = text;
    }

    public Object getChecker() {
      return check;
    }

    public Integer getLine() {
      return line;
    }

    public String getText(Locale locale) {
      return text;
    }
  }
}
