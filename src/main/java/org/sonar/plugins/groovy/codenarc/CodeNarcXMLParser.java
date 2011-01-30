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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.groovy.GroovyConstants;
import org.sonar.plugins.groovy.foundation.GroovyFile;
import org.sonar.plugins.groovy.utils.GroovyUtils;

import java.io.File;

import javax.xml.stream.XMLStreamException;

public class CodeNarcXMLParser {

  private SensorContext context;
  private RuleFinder ruleFinder;

  public CodeNarcXMLParser(SensorContext context, RuleFinder ruleFinder) {
    this.context = context;
    this.ruleFinder = ruleFinder;
  }

  public void parseAndLogCodeNarcResults(File xmlFile) {
    GroovyUtils.LOG.info("Parsing {}", xmlFile);

    StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        SMInputCursor pack = rootCursor.descendantElementCursor("Package");

        while (pack.getNext() != null) {
          String packPath = pack.getAttrValue("path");
          SMInputCursor file = pack.descendantElementCursor("File");
          while (file.getNext() != null) {
            String fileName = packPath + "/" + FilenameUtils.removeExtension(file.getAttrValue("name"));
            SMInputCursor violation = file.childElementCursor("Violation");
            while (violation.getNext() != null) {
              String lineNumber = violation.getAttrValue("lineNumber");
              String checkKey = violation.getAttrValue("ruleName");

              log(checkKey, fileName, Integer.parseInt(lineNumber), "");
            }
          }
        }
      }
    });
    try {
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      GroovyUtils.LOG.error("Error parsing file : " + xmlFile);
    }
  }

  void log(String checkKey, String filename, Integer line, String message) {
    RuleQuery ruleQuery = RuleQuery.create()
        .withRepositoryKey(GroovyConstants.REPOSITORY_KEY)
        .withConfigKey(checkKey);
    Rule rule = ruleFinder.find(ruleQuery);
    if (rule != null) {
      GroovyFile sonarFile = new GroovyFile(StringUtils.replace(filename, "/", "."));
      Violation violation = Violation.create(rule, sonarFile).setLineId(line).setMessage(message);
      context.saveViolation(violation);
    }
  }
}
