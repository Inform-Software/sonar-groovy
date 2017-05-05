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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.codenarc.analyzer.AbstractSourceAnalyzer;
import org.codenarc.results.DirectoryResults;
import org.codenarc.results.FileResults;
import org.codenarc.results.Results;
import org.codenarc.rule.Violation;
import org.codenarc.ruleset.RuleSet;
import org.codenarc.source.SourceFile;
import org.sonar.api.batch.fs.InputFile;

public class CodeNarcSourceAnalyzer extends AbstractSourceAnalyzer {

  private final Map<InputFile, List<Violation>> violationsByFile = new HashMap<>();
  private final List<InputFile> sourceFiles;

  public CodeNarcSourceAnalyzer(List<InputFile> sourceFiles) {
    this.sourceFiles = sourceFiles;
  }

  @Override
  public Results analyze(RuleSet ruleSet) {
    Map<File, List<FileResults>> resultsByFileByDirectory = processFiles(ruleSet);
    DirectoryResults directoryResults = new DirectoryResults(".");
    for (List<FileResults> fileResults : resultsByFileByDirectory.values()) {
      fileResults.forEach(directoryResults::addChild);
    }
    return directoryResults;
  }

  private Map<File, List<FileResults>> processFiles(RuleSet ruleSet) {
    Map<File, List<FileResults>> results = new HashMap<>();
    for (InputFile inputFile : sourceFiles) {
      List<Violation> violations = collectViolations(new SourceFile(inputFile.file()), ruleSet);
      violationsByFile.put(inputFile, violations);
      FileResults result = new FileResults(inputFile.absolutePath(), violations);
      results.putIfAbsent(inputFile.file().getParentFile(), new LinkedList<>());
      results.get(inputFile.file().getParentFile()).add(result);
    }
    return results;
  }

  @Override
  public List<?> getSourceDirectories() {
    return new ArrayList<>();
  }

  public Map<InputFile, List<Violation>> getViolationsByFile() {
    return violationsByFile;
  }

}
