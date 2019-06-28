/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2010-2019 SonarSource SA & Community
 * mailto:info AT sonarsource DOT com
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
package org.sonar.plugins.groovy.codenarc.apt;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.groovy.codenarc.RuleParameter;

public class AptParser {

  private static final Logger log = LoggerFactory.getLogger(AptParser.class);

  private static final String LIST_PREFIX = "* {{";
  private static final String NEW_RULE_PREFIX = "* ";
  private static final String PARAMETER_START_SEPARATOR = "\\*(-)+\\+(-)+\\+(-)+\\+";
  private static final String PARAMETER_SEPARATOR = "\\+((-)+\\+)+";
  private static final String PARAMETER_CONTENT = "(\\|(.)*)+";
  private static final String EXAMPLE_SEPARATOR_1 = "(-)+";
  private static final String EXAMPLE_SEPARATOR_2 = "\\+(-)+";

  public Map<String, AptResult> parse(List<File> files) throws IOException {
    Map<String, AptResult> results = Maps.newHashMap();
    if (!files.isEmpty()) {
      for (File file : files) {
        Map<String, AptResult> parametersByFile = readFile(file);
        mergeParameters(results, parametersByFile);
      }
    }
    return results;
  }

  private Map<String, AptResult> readFile(File file) throws IOException {
    Map<String, AptResult> results = Maps.newHashMap();
    List<String> lines = Files.readAllLines(file.toPath());

    boolean inRule = false;
    boolean inParameters = false;
    boolean inExample = false;
    String currentRule = null;
    AptResult currentResult = null;
    RuleParameter currentParameter = null;
    int[] splits = new int[3];
    for (String fullLine : lines) {
      String line = fullLine.trim();
      if (fullLine.startsWith(NEW_RULE_PREFIX) && !line.startsWith(LIST_PREFIX) && inRule) {
        results.put(currentRule, currentResult);
        inRule = false;
        currentRule = null;
        currentResult = null;
      }
      if (!inRule && line.startsWith(NEW_RULE_PREFIX)) {
        currentRule = getRuleName(line.trim());
        inRule = currentRule != null;
        if (inRule) {
          currentResult = results.get(currentRule);
          if (currentResult == null) {
            currentResult = new AptResult(currentRule);
          }
        }
      } else if (inRule && !inExample && isExampleSeparator(line)) {
        inExample = true;
        currentResult.appendDescription("<pre>\n");
      } else if (inRule && !inExample && !inParameters && isValidDescriptionLine(line)) {
        currentResult.addParagraphLine(cleanDescription(line));
      } else if (inRule && inExample && isExampleSeparator(line)) {
        inExample = false;
        currentResult.appendDescription("</pre>\n");
      } else if (inRule && inExample) {
        currentResult.appendDescription(cleanExample(fullLine) + "\n");
      } else if (inRule && !inParameters && line.matches(PARAMETER_START_SEPARATOR)) {
        inParameters = true;
        currentParameter = RuleParameter.createEmpty();
        splits[0] = line.indexOf('*') + 1;
        splits[1] = line.indexOf('+', splits[0]) + 1;
        splits[2] = line.indexOf('+', splits[1]) + 1;
      } else if (inRule && inParameters && (line.matches(PARAMETER_CONTENT))) {
        String[] blocks = new String[3];
        blocks[0] = line.substring(splits[0], splits[1] - 1);
        blocks[1] = line.substring(splits[1], splits[2] - 1);
        blocks[2] = line.substring(splits[2], line.length() - 1);
        if (blocks.length == 3 && !isHeaderLine(blocks)) {
          String key = blocks[0].trim();
          String description = blocks[1].trim();
          String defaultValue = blocks[2].trim();
          if (StringUtils.isNotBlank(key)) {
            currentParameter = currentParameter.withExpandedKey(key.replaceAll("(-)+", ""));
          }
          if (StringUtils.isNotBlank(defaultValue) && !currentParameter.hasDefaultValue()) {
            currentParameter =
                currentParameter.withNewDefaultValue(cleanDefaultValue(defaultValue));
          }
          if (StringUtils.isNotBlank(description)) {
            currentParameter =
                currentParameter.withExpandedDescription(cleanParameter(description));
          }
        }
      } else if (inRule && inParameters && isParameterSeparator(line)) {
        if (!currentParameter.isEmpty()) {
          currentResult.parameters.add(currentParameter);
        }
        currentParameter = RuleParameter.createEmpty();
      } else if (inRule && inParameters) {
        if (!currentParameter.isEmpty()) {
          currentResult.parameters.add(currentParameter);
        }
        currentParameter = RuleParameter.createEmpty();
        inParameters = false;
      }
    }

    // last item
    if (inRule) {
      results.put(currentRule, currentResult);
    }

    return results;
  }

  private static boolean isExampleSeparator(String line) {
    return line.matches(EXAMPLE_SEPARATOR_1) || line.matches(EXAMPLE_SEPARATOR_2);
  }

  private static boolean isValidDescriptionLine(String line) {
    return !startsWith(line, "<Since", "~~~", "<New", "** ", "[]", "*----", "+----", "|")
        && !isParameterSeparator(line);
  }

  private static boolean startsWith(String line, String... prefixes) {
    for (String prefix : prefixes) {
      if (line.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isParameterSeparator(String line) {
    return line.matches(PARAMETER_SEPARATOR) || line.matches(PARAMETER_START_SEPARATOR);
  }

  private static String cleanDescription(String description) {
    String result = " " + description + " ";
    // This is a bit stupid
    result = result.replaceAll("&", "&amp;");
    result = result.replaceAll(" <=> ", " &lt;=&gt; ");
    result = result.replaceAll(" <<<= ", " &lt;&lt;&lt;= ");
    result = result.replaceAll(" >>>= ", " &gt;&gt;&gt;= ");
    result = result.replaceAll(" <<= ", " &lt;&lt;= ");
    result = result.replaceAll(" >>= ", " &gt;&gt;= ");
    result = result.replaceAll(" <= ", " &lt;= ");
    result = result.replaceAll(" >= ", " &gt;= ");
    result = result.replaceAll(" < ", " &lt; ");
    result = result.replaceAll(" > ", " &gt; ");
    result = result.replaceAll("->", "-&gt;");
    result = result.replaceAll("\\\\=", "=");
    result = result.replaceAll("\\\\<", "&lt;");
    result = result.replaceAll("\\\\>", "&gt;");

    result = result.replaceAll("<", "\uE000");
    result = result.replaceAll(">", "\uE001");

    result = result.replaceAll("\uE000\uE000\uE000", "<code>");
    result = result.replaceAll("\uE000\uE000", "<b>");
    result = result.replaceAll("\uE000", "<i>");
    result = result.replaceAll("\uE001\uE001\uE001", "</code>");
    result = result.replaceAll("\uE001\uE001", "</b>");
    result = result.replaceAll("\uE001", "</i>");
    return result.trim();
  }

  private static String cleanParameter(String param) {
    return param.replaceAll("<", "").replaceAll(">", "");
  }

  private String cleanExample(String line) {
    return line.replaceAll("&", "&amp;").replaceAll("\\\\?<", "&lt;").replaceAll("\\\\?>", "&gt;");
  }

  private static String cleanDefaultValue(String defaultValue) {
    String result = defaultValue.replaceAll("<<<", "");
    result = result.replaceAll("<<", "");
    result = result.replaceAll(">>>", "");
    result = result.replaceAll(">>", "");
    if (isBetween(result, "'", "'")
        || isBetween(result, "<", ">")
        || isBetween(result, "\"", "\"")
        || isBetween(result, "/", "/")) {
      result = result.substring(1, result.length() - 1);
    }
    return result;
  }

  private static boolean isBetween(String parameter, String prefix, String suffix) {
    return parameter.startsWith(prefix) && parameter.endsWith(suffix);
  }

  private static boolean isHeaderLine(String[] blocks) {
    return "<<Property>>".equalsIgnoreCase(blocks[0].trim());
  }

  private static String getRuleName(String line) {
    if (StringUtils.isBlank(line)) {
      return null;
    }
    String result = null;
    if (line.startsWith("* {")) {
      result = StringUtils.substringBetween(line, "* {", "} Rule").trim();
    } else {
      result = line.substring(2).trim();
    }
    if (result.endsWith("Rule")) {
      result = result.substring(0, result.length() - 4);
    }
    if (StringUtils.isAllLowerCase(result)
        || !StringUtils.isAlphanumeric(result)
        || "References".equals(result)) {
      // false positive
      return null;
    }
    return result;
  }

  private static void mergeParameters(
      Map<String, AptResult> results, Map<String, AptResult> parametersByFile) {
    for (Entry<String, AptResult> parametersByFileEntry : parametersByFile.entrySet()) {
      String rule = parametersByFileEntry.getKey();
      AptResult currentRuleResult = results.get(rule);
      if (currentRuleResult == null) {
        currentRuleResult = new AptResult(rule);
      }
      AptResult resultForRuleInFile = parametersByFileEntry.getValue();
      if (resultForRuleInFile.parameters != null) {
        for (RuleParameter parameter : resultForRuleInFile.getParameters()) {
          if (!currentRuleResult.parameters.contains(parameter)) {
            currentRuleResult.parameters.add(parameter);
          }
        }
      }
      boolean alreadyHasExample = StringUtils.isNotBlank(currentRuleResult.getDescription());
      boolean provideNewExample = StringUtils.isNotBlank(resultForRuleInFile.getDescription());
      if (!alreadyHasExample && provideNewExample) {
        currentRuleResult.replaceDescription(resultForRuleInFile);
      } else if (alreadyHasExample && provideNewExample) {
        log.info("CONFLICT RULE: {}", rule);
        log.info(currentRuleResult.getDescription());
        log.info("WITH");
        log.info(resultForRuleInFile.getDescription());
      }
      results.put(rule, currentRuleResult);
    }
  }
}
