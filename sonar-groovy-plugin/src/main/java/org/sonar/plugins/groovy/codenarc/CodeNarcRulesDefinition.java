/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2016 SonarSource SA
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.squidbridge.rules.SqaleXmlLoader;

public class CodeNarcRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = Groovy.KEY;
  public static final String REPOSITORY_NAME = "CodeNarc";

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, Groovy.KEY)
      .setName(REPOSITORY_NAME);

    RulesDefinitionXmlLoader ruleLoader = new RulesDefinitionXmlLoader();
    ruleLoader.load(repository, CodeNarcRulesDefinition.class.getResourceAsStream("/org/sonar/plugins/groovy/rules.xml"), "UTF-8");
    SqaleXmlLoader.load(repository, "/com/sonar/sqale/groovy-model.xml");
    repository.done();
  }
}
