/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.codenarc.analyzer.AbstractSourceAnalyzer;
import org.codenarc.results.DirectoryResults;
import org.codenarc.results.FileResults;
import org.codenarc.results.Results;
import org.codenarc.rule.Violation;
import org.codenarc.ruleset.RuleSet;
import org.codenarc.source.SourceFile;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CodeNarcSourceAnalyzer extends AbstractSourceAnalyzer {

  private final Map<File, List<Violation>> violationsByFile = Maps.newHashMap();
  private final List<File> sourceFiles;

  public CodeNarcSourceAnalyzer(List<File> sourceFiles) {
    this.sourceFiles = sourceFiles;
  }

  @Override
  public Results analyze(RuleSet ruleSet) {
    Multimap<File, FileResults> resultsByFileByDirectory = processFiles(ruleSet);
    DirectoryResults directoryResults = new DirectoryResults(".");
    for (FileResults fileResults : resultsByFileByDirectory.values()) {
      directoryResults.addChild(fileResults);
    }
    return directoryResults;
  }

  private Multimap<File, FileResults> processFiles(RuleSet ruleSet) {
    Multimap<File, FileResults> results = LinkedListMultimap.create();
    for (File file : sourceFiles) {
      List<Violation> violations = collectViolations(new SourceFile(file), ruleSet);
      violationsByFile.put(file, violations);
      FileResults result = new FileResults(file.getAbsolutePath(), violations);
      results.put(file.getParentFile(), result);
    }
    return results;
  }

  @Override
  public List getSourceDirectories() {
    return ImmutableList.of();
  }

  public Map<File, List<Violation>> getViolationsByFile() {
    return violationsByFile;
  }

}
