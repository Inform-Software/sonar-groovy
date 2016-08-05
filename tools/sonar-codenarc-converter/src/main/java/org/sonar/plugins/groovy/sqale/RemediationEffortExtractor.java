/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2011-2016 SonarSource SA
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
package org.sonar.plugins.groovy.sqale;

import org.apache.commons.io.Charsets;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemediationEffortExtractor {
  private static final String SQALE_MODEL_LOCATION = "src/test/files/groovy-model.xml";
  private static final String REMEDIATION_FILE_LOCATION = "target/cost.csv";

  private List<ExtractedRule> extractedRules = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    RemediationEffortExtractor extractor = new RemediationEffortExtractor();
    extractor.extractFromSqaleFile(SQALE_MODEL_LOCATION);
    extractor.toCSV(REMEDIATION_FILE_LOCATION);
  }

  public void extractFromSqaleFile(String sqaleModelLocation) throws Exception {
    Document sqaleModel = parseXml(new File(sqaleModelLocation));
    Node sqale = sqaleModel.getFirstChild();
    NodeList categories = sqale.getChildNodes();
    handleCategories(categories);
  }

  public void toCSV(String remediationFileLocation) {
    File file = new File(remediationFileLocation);
    if (!file.exists() || file.delete()) {
      try {
        Path path = Paths.get(file.getAbsolutePath());
        Files.write(path, getLines(), Charsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create file at this location: " + file.getAbsolutePath(), e);
      }
    } else {
      throw new IllegalStateException("Unable to create file at this location: " + file.getAbsolutePath());
    }
  }

  public List<ExtractedRule> extractedRules() {
    return extractedRules;
  }

  private List<String> getLines() {
    List<String> rules = extractedRules.stream().sorted().map(ExtractedRule::toCSV).collect(Collectors.toList());
    rules.add(0, ExtractedRule.csvHeader());
    return rules;
  }

  private void handleCategories(NodeList categories) {
    for (int c = 0; c < categories.getLength(); c++) {
      Node category = categories.item(c);
      NodeList childNodes = category.getChildNodes();
      String categoryName = "UNKNOWN";
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node child = childNodes.item(i);
        String nodeName = child.getNodeName();
        if ("key".equals(nodeName)) {
          categoryName = getTextValue(child);
        } else if ("chc".equals(nodeName)) {
          handleSubCategories(child, categoryName);
        }
      }
    }
  }

  private void handleSubCategories(Node subCategory, String categoryName) {
    NodeList childNodes = subCategory.getChildNodes();
    String subCategoryName = "UNKNOWN";
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      String nodeName = child.getNodeName();
      if ("key".equals(nodeName)) {
        subCategoryName = getTextValue(child);
      } else if ("chc".equals(nodeName)) {
        ExtractedRule extractedRule = extractRule(child, categoryName, subCategoryName);
        if (extractedRule != null) {
          extractedRules.add(extractedRule);
        }
      }
    }
  }

  private static ExtractedRule extractRule(Node rule, String categoryName, String subCategoryName) {
    NodeList childNodes = rule.getChildNodes();
    ExtractedRule extractedRule = new ExtractedRule(categoryName, subCategoryName);
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      String nodeName = child.getNodeName();
      if ("rule-key".equals(nodeName)) {
        extractedRule.ruleKey = getTextValue(child);
        if (!extractedRule.ruleKey.startsWith("org.codenarc.")) {
          // skip common rules
          return null;
        }
      } else if ("prop".equals(nodeName)) {
        handleProperty(extractedRule, child);
      }
    }
    return extractedRule;
  }

  private static void handleProperty(ExtractedRule extractedRule, Node node) {
    NodeList childNodes = node.getChildNodes();
    Property property = Property.OTHER;
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      String nodeName = child.getNodeName();
      if ("key".equals(nodeName)) {
        property = Property.getProp(getTextValue(child));
      } else if ("val".equals(nodeName) || "txt".equals(nodeName)) {
        extractedRule.setProperty(property, getTextValue(child));
      }
    }
  }

  private enum Property {
    REMEDIATION_FUNCTION, REMEDIATION_FACTOR, OFFSET, OTHER;

  private static Property getProp(String s) {
      if ("remediationFunction".equals(s)) {
        return REMEDIATION_FUNCTION;
      } else if ("remediationFactor".equals(s)) {
        return REMEDIATION_FACTOR;
      } else if ("offset".equals(s)) {
        return OFFSET;
      }
      return OTHER;
    }
  }

  public static class ExtractedRule implements Comparable<ExtractedRule> {
    final String category;
    final String subCategory;

    String ruleKey;
    String remediationFunction = "";
    String remediationFactor = "";
    String offset = "";

    public ExtractedRule(String category, String subCategory) {
      this.category = category;
      this.subCategory = subCategory;
    }

    public void setProperty(Property property, String textValue) {
      switch (property) {
        case REMEDIATION_FACTOR:
          remediationFactor += cleanText(textValue);
          break;
        case REMEDIATION_FUNCTION:
          remediationFunction += textValue;
          break;
        case OFFSET:
          offset += textValue;
          break;
        default:
          // do nothing
          break;
      }
    }

    private static String cleanText(String textValue) {
      String newValue = textValue;
      if (textValue.endsWith(".0")) {
        newValue = textValue.substring(0, textValue.length() - 2);
      } else if ("mn".equals(textValue)) {
        newValue = "min";
      }
      return newValue;
    }

    @Override
    public int compareTo(ExtractedRule o) {
      return ruleKey.compareTo(o.ruleKey);
    }

    public String toCSV() {
      return ruleKey + ";" + remediationFunction + ";" + remediationFactor;
    }

    public static String csvHeader() {
      return "ruleKey;remediationFunction;remediationFactor";
    }
  }

  private static String getTextValue(Node node) {
    return node.getFirstChild().getTextContent();
  }

  private static Document parseXml(File f) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder;
    documentBuilder = documentBuilderFactory.newDocumentBuilder();
    InputSource is = new InputSource();
    is.setCharacterStream(new FileReader(f));
    return documentBuilder.parse(is);
  }
}
