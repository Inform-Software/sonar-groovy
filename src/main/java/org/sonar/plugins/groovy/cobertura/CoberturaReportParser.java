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
package org.sonar.plugins.groovy.cobertura;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import javax.annotation.CheckForNull;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;

public class CoberturaReportParser {

  private final SensorContext context;
  private final FileSystem fileSystem;
  private List<String> sourceDirs = Lists.newArrayList();

  public CoberturaReportParser(final SensorContext context, final FileSystem fileSystem) {
    this.context = context;
    this.fileSystem = fileSystem;
  }

  /**
   * Parse a Cobertura xml report and create measures accordingly
   */
  public void parseReport(File xmlFile) {
    try {
      parseSources(xmlFile);
      parsePackages(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private void parseSources(File xmlFile) throws XMLStreamException {
    StaxParser sourceParser = new StaxParser(new StaxParser.XmlStreamHandler() {
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        sourceDirs = collectSourceDirs(rootCursor.descendantElementCursor("source"));
      }
    });
    sourceParser.parse(xmlFile);
  }

  private List<String> collectSourceDirs(SMInputCursor source) throws XMLStreamException {
    List<String> sourceDirs = Lists.newLinkedList();
    while (source.getNext() != null) {
      String sourceDir = cleanSourceDir(source.getElemStringValue());
      if (StringUtils.isNotBlank(sourceDir) && !"--source".equals(sourceDir)) {
        sourceDirs.add(sourceDir);
      }
    }
    return sourceDirs;
  }

  private String cleanSourceDir(String sourceDir) {
    if (StringUtils.isNotBlank(sourceDir)) {
      return sourceDir.trim();
    }
    return sourceDir;
  }

  private void parsePackages(File xmlFile) throws XMLStreamException {
    StaxParser fileParser = new StaxParser(new StaxParser.XmlStreamHandler() {
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        collectPackageMeasures(rootCursor.descendantElementCursor("package"), sourceDirs);
      }
    });
    fileParser.parse(xmlFile);
  }

  private void collectPackageMeasures(SMInputCursor pack, List<String> sourceDirs) throws XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {
        InputFile file = getInputFile(entry.getKey(), sourceDirs);
        if (file != null) {
          for (Measure measure : entry.getValue().createMeasures()) {
            context.saveMeasure(file, measure);
          }
        }
      }
    }
  }

  @CheckForNull
  private InputFile getInputFile(String filename, List<String> sourceDirs) {
    for (String sourceDir : sourceDirs) {
      String fileAbsolutePath = sourceDir + "/" + filename;
      InputFile file = fileSystem.inputFile(fileSystem.predicates().hasAbsolutePath(fileAbsolutePath));
      if (file != null) {
        return file;
      }
    }
    return null;
  }

  private void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> builderByFilename) throws XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = clazz.getAttrValue("filename");
      CoverageMeasuresBuilder builder = builderByFilename.get(fileName);
      if (builder == null) {
        builder = CoverageMeasuresBuilder.create();
        builderByFilename.put(fileName, builder);
      }
      collectFileData(clazz, builder);
    }
  }

  private void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      try {
        builder.setHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));
      } catch (ParseException e) {
        throw new XmlParserException(e);
      }

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }
}
