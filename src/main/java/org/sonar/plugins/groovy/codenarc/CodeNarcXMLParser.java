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

import org.apache.commons.io.FilenameUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.groovy.utils.GroovyUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;

public class CodeNarcXMLParser {
  private GroovyMessageDispatcher messageDispatcher;

  public CodeNarcXMLParser(GroovyMessageDispatcher messageDispatcher) {
    this.messageDispatcher = messageDispatcher;
  }

  public void parseAndLogCodeNarcResults(File xmlFile) {
    GroovyUtils.LOG.info("parsing {}", xmlFile);

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

              messageDispatcher.log(checkKey, fileName, Integer.parseInt(lineNumber), "");
            }
          }
        }
      }
    }
    );
    try {
      parser.parse(xmlFile);
    }
    catch (XMLStreamException e) {
      GroovyUtils.LOG.error("Error parsing file : " + xmlFile);
    }
  }
}