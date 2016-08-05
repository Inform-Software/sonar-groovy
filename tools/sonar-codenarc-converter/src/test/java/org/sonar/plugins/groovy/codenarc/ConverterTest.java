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
package org.sonar.plugins.groovy.codenarc;

import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.plugins.groovy.codenarc.printer.XMLPrinter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class ConverterTest {

  private static final String PLUGIN_RULES_FILE_LOCATION = "../../sonar-groovy-plugin/src/main/resources/org/sonar/plugins/groovy/rules.xml";

  @org.junit.Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void test_xml_equivalence() throws Exception {
    assertSimilarXml(getGeneratedXmlRulesFile(), new File(PLUGIN_RULES_FILE_LOCATION));
  }

  static void showDelta(String ruleName, String s1, String s2) {
    showDelta(ruleName, Lists.newArrayList(s1), Lists.newArrayList(s2));
  }

  static void showDelta(String ruleName, List<String> s1, List<String> s2) {
    System.out.println("------------------------------------------------------------------------------------------");
    System.out.println("DIFFERENCE! " + ruleName);
    Patch p = DiffUtils.diff(s1, s2);
    for (Delta delta : p.getDeltas()) {
      System.out.println(delta);
    }
  }

  private File getGeneratedXmlRulesFile() throws Exception {
    File generatedRules = tmpDir.newFolder("xml");
    return new XMLPrinter().init(new Converter()).process(Converter.loadRules()).printAll(generatedRules);
  }

  private static void assertSimilarXml(File generatedRulesXML, File rulesFromPluginXML) throws Exception {
    int nbrDiff = 0;
    int nbrMissing = 0;

    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document generatedDoc = dBuilder.parse(generatedRulesXML);
    Document pluginDoc = dBuilder.parse(rulesFromPluginXML);

    NodeList generatedNodes = generatedDoc.getChildNodes().item(1).getChildNodes();
    NodeList pluginNodes = pluginDoc.getChildNodes().item(1).getChildNodes();

    for (int i = 0; i < generatedNodes.getLength(); i++) {
      Node generatedRule = generatedNodes.item(i);
      Node pluginRule = null;
      short generatedNodeType = generatedRule.getNodeType();
      if (generatedNodeType != Node.COMMENT_NODE && generatedNodeType != Node.TEXT_NODE) {
        boolean found = false;
        boolean diff = false;
        for (int j = 0; j < pluginNodes.getLength(); j++) {
          pluginRule = pluginNodes.item(j);
          short pluginNodeType = pluginRule.getNodeType();
          if (pluginNodeType != Node.COMMENT_NODE && pluginNodeType != Node.TEXT_NODE) {
            if (generatedRule.isEqualNode(pluginRule)) {
              found = true;
              break;
            } else if (getRuleKey(generatedRule).equals(getRuleKey(pluginRule))) {
              diff = true;
              break;
            }
          }
        }
        if (diff) {
          nbrDiff++;
          String generatedRuleString = nodeToString(generatedRule);
          String pluginRuleString = nodeToString(pluginRule);
          showDelta(getRuleKey(generatedRule), Lists.newArrayList(generatedRuleString.split("\\r?\\n")), Lists.newArrayList(pluginRuleString.split("\\r?\\n")));
        } else if (!found) {
          nbrMissing++;
          System.out.println("------------------------------------------------------------------------------------------");
          System.out.println("NOT FOUND! " + getRuleKey(generatedRule));
        }
      }
    }

    Assert.assertEquals(0, nbrMissing);
    /*
     * Known diffs:
     * - MisorderedStaticImportsRule : description of 'comesBefore' parameter missing in apt files
     * - FileCreateTempFileRule: link to website
     * - BracesForIfElseRule: default value of parameters should be true, not 'the same as sameLine'
     */
    Assert.assertEquals(3, nbrDiff);
  }

  private static String getRuleKey(Node Rule) {
    return Rule.getChildNodes().item(1).getFirstChild().getNodeValue();
  }

  private static String nodeToString(Node node) {
    StringWriter sw = new StringWriter();
    try {
      Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.setOutputProperty(OutputKeys.INDENT, "yes");
      t.transform(new DOMSource(node), new StreamResult(sw));
    } catch (TransformerException te) {
      System.out.println("nodeToString Transformer Exception");
    }
    return sw.toString();
  }
}
