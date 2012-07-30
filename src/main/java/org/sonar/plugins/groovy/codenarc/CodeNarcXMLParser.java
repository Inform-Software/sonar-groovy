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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.util.List;

public final class CodeNarcXMLParser implements StaxParser.XmlStreamHandler {

  public static List<CodeNarcViolation> parse(File file) {
    CodeNarcXMLParser handler = new CodeNarcXMLParser();
    try {
      new StaxParser(handler).parse(file);
    } catch (XMLStreamException e) {
      throw new SonarException("Unabel to parse file: " + file, e);
    }
    return handler.result.build();
  }

  private final ImmutableList.Builder<CodeNarcViolation> result = ImmutableList.builder();

  private CodeNarcXMLParser() {
  }

  public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
    rootCursor.advance();
    SMInputCursor pack = rootCursor.descendantElementCursor("Package");

    while (pack.getNext() != null) {
      String packPath = pack.getAttrValue("path");
      SMInputCursor file = pack.descendantElementCursor("File");
      while (file.getNext() != null) {
        String filename = packPath + "/" + file.getAttrValue("name");
        SMInputCursor violation = file.childElementCursor("Violation");
        while (violation.getNext() != null) {
          String lineNumber = violation.getAttrValue("lineNumber");
          String ruleName = violation.getAttrValue("ruleName");

          SMInputCursor messageCursor = violation.childElementCursor("Message");
          String message = messageCursor.getNext() == null ? "" : messageCursor.collectDescendantText(true);

          result.add(new CodeNarcViolation(ruleName, filename, lineNumber, message));
        }
      }
    }
  }

  public static class CodeNarcViolation {
    private final String ruleName;
    private final String filename;
    private final Integer line;
    private final String message;

    public CodeNarcViolation(String ruleName, String filename, String lineNumber, String message) {
      this.ruleName = ruleName;
      this.filename = filename;
      this.line = StringUtils.isBlank(lineNumber) ? null : Integer.parseInt(lineNumber);
      this.message = message;
    }

    public String getRuleName() {
      return ruleName;
    }

    public String getFilename() {
      return filename;
    }

    public Integer getLine() {
      return line;
    }

    public String getMessage() {
      return message;
    }

  }

}
