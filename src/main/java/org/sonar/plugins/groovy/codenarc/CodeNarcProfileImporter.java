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

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

public class CodeNarcProfileImporter extends ProfileImporter {
  private static final String RULE_NODE = "rule";
  private static final String RULE_CLASS_ATTR = "class";
  private static final String PROPERTY_NODE = "property";
  private static final String PROPERTY_NAME_ATTR = "name";
  private static final String PROPERTY_VALUE_ATTR = "value";

  private RuleFinder ruleFinder;

  public CodeNarcProfileImporter(RuleFinder ruleFinder) {
    super(CodeNarcConstants.REPOSITORY_KEY, CodeNarcConstants.REPOSITORY_NAME);
    setSupportedLanguages(Groovy.KEY);
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
    SMInputFactory inputFactory = initStax();
    RulesProfile profile = RulesProfile.create();

    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <ruleset>
      SMInputCursor ruleCursor = rootC.childElementCursor(RULE_NODE);
      while (ruleCursor.getNext() != null) {
        String ruleKey = ruleCursor.getAttrValue(RULE_CLASS_ATTR);
        Rule rule = ruleFinder.findByKey(CodeNarcConstants.REPOSITORY_KEY, ruleKey);
        if (rule == null) {
          messages.addWarningText("CodeNarc rule '" + ruleKey + "' not found");
        } else {
          ActiveRule activeRule = profile.activateRule(rule, null);
          processProperties(ruleCursor, activeRule);
        }
      }

    } catch (XMLStreamException e) {
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }
    return profile;
  }

  private void processProperties(SMInputCursor ruleCursor, ActiveRule activeRule) throws XMLStreamException {
    SMInputCursor propertyCursor = ruleCursor.childElementCursor(PROPERTY_NODE);
    while (propertyCursor.getNext() != null) {
      String key = propertyCursor.getAttrValue(PROPERTY_NAME_ATTR);
      String value = propertyCursor.getAttrValue(PROPERTY_VALUE_ATTR);
      activeRule.setParameter(key, value);
    }
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

}
