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
package org.sonar.plugins.groovy.codenarc.printer;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.groovy.codenarc.Converter;
import org.sonar.plugins.groovy.codenarc.Rule;
import org.sonar.plugins.groovy.codenarc.RuleParameter;
import org.sonar.plugins.groovy.codenarc.RuleSet;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class XMLPrinter implements Printer {

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private Converter converter;
  private String resultAsXML;

  @Override
  public XMLPrinter init(Converter converter) {
    this.converter = converter;
    return this;
  }

  @Override
  public XMLPrinter process(Multimap<RuleSet, Rule> rulesBySet) throws Exception {
    StringBuilder xmlStringBuilder = new StringBuilder();

    String version = IOUtils.toString(Converter.class.getResourceAsStream("/codenarc-version.txt"));
    xmlStringBuilder.append("<!-- Generated using CodeNarc " + version + " -->");
    xmlStringBuilder.append(LINE_SEPARATOR);

    start(xmlStringBuilder);

    for (RuleSet ruleSet : RuleSet.values()) {
      startSet(xmlStringBuilder, ruleSet.getLabel());
      ArrayList<Rule> rules = Lists.newArrayList(rulesBySet.get(ruleSet));
      for (Rule rule : rules) {
        converter.startPrintingRule(rule);
        printAsXML(rule, xmlStringBuilder);
      }
    }
    end(xmlStringBuilder);
    this.resultAsXML = xmlStringBuilder.toString();
    return this;
  }

  public String generatedXML() {
    return resultAsXML;
  }

  @Override
  public File printAll(File resultDir) throws Exception {
    File resultFile = setUpRulesFile(resultDir);
    PrintStream out = new PrintStream(resultFile, "UTF-8");
    out.print(resultAsXML);
    out.flush();
    out.close();
    return resultFile;
  }

  private static File setUpRulesFile(File resultDir) throws IOException {
    File rules = new File(resultDir, "rules.xml");
    if (rules.exists()) {
      rules.delete();
    }
    rules.createNewFile();
    return rules;
  }

  private static void startSet(StringBuilder xmlStringBuilder, String name) {
    xmlStringBuilder.append("  <!-- " + name + " rules -->");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append(LINE_SEPARATOR);
  }

  private static void start(StringBuilder xmlStringBuilder) {
    xmlStringBuilder.append("<rules>");
    xmlStringBuilder.append(LINE_SEPARATOR);
  }

  private static void end(StringBuilder xmlStringBuilder) {
    xmlStringBuilder.append("</rules>");
    xmlStringBuilder.append(LINE_SEPARATOR);
  }

  /**
   * Rule format based on {@link org.sonar.api.server.rule.RulesDefinitionXmlLoader}
   */
  private static void printAsXML(Rule rule, StringBuilder xmlStringBuilder) {
    if (rule.version != null) {
      xmlStringBuilder.append("  <!-- since " + rule.version + " -->");
      xmlStringBuilder.append(LINE_SEPARATOR);
    }
    xmlStringBuilder.append("  <rule>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append("    <key>" + rule.fixedRuleKey() + "</key>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append("    <severity>" + rule.severity + "</severity>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append("    <name><![CDATA[" + rule.name + "]]></name>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append("    <internalKey><![CDATA[" + rule.internalKey + "]]></internalKey>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append("    <description><![CDATA[" + rule.description + "]]></description>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    if (!rule.tags.isEmpty()) {
      for (String tag : rule.tags) {
        xmlStringBuilder.append("    <tag>" + tag + "</tag>");
        xmlStringBuilder.append(LINE_SEPARATOR);
      }
    }

    if (!rule.parameters.isEmpty()) {
      List<RuleParameter> sortedParameters = Lists.newArrayList(rule.parameters);
      Collections.sort(sortedParameters, new Comparator<RuleParameter>() {
        @Override
        public int compare(RuleParameter o1, RuleParameter o2) {
          return o1.key.compareTo(o2.key);
        }
      });
      for (RuleParameter parameter : sortedParameters) {
        xmlStringBuilder.append("    <param>");
        xmlStringBuilder.append(LINE_SEPARATOR);
        xmlStringBuilder.append("      <key>" + parameter.key + "</key>");
        xmlStringBuilder.append(LINE_SEPARATOR);
        if (StringUtils.isNotBlank(parameter.description)) {
          xmlStringBuilder.append("      <description><![CDATA[" + parameter.description + "]]></description>");
          xmlStringBuilder.append(LINE_SEPARATOR);
        }
        if (StringUtils.isNotBlank(parameter.defaultValue) && !"null".equals(parameter.defaultValue)) {
          xmlStringBuilder.append("      <defaultValue>" + parameter.defaultValue + "</defaultValue>");
          xmlStringBuilder.append(LINE_SEPARATOR);
        }
        xmlStringBuilder.append("    </param>");
        xmlStringBuilder.append(LINE_SEPARATOR);
      }
    }

    xmlStringBuilder.append("  </rule>");
    xmlStringBuilder.append(LINE_SEPARATOR);
    xmlStringBuilder.append(LINE_SEPARATOR);
  }

}
