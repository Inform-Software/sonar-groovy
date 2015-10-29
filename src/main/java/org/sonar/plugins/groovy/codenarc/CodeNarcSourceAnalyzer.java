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
import java.io.FileFilter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CodeNarcSourceAnalyzer extends AbstractSourceAnalyzer {

  private final Map<File, List<Violation>> violationsByFile = Maps.newHashMap();
  private final List<File> sourceFiles;
  private final File baseDir;
  private static final FileFilter SUBDIRECTORY_FILTER = new SubDirectoryFilter();

  public CodeNarcSourceAnalyzer(List<File> sourceFiles, File baseDir) {
    this.sourceFiles = sourceFiles;
    this.baseDir = baseDir;
  }

  @Override
  public Results analyze(RuleSet ruleSet) {
    Multimap<File, FileResults> resultsByFileByDirectory = processFiles(ruleSet);
    return processDirectories(baseDir, resultsByFileByDirectory);
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

  private static DirectoryResults processDirectories(File baseDir, Multimap<File, FileResults> resultsByFileByDirectory) {
    Map<File, DirectoryResults> results = prepopulateDirectoryResults(baseDir, resultsByFileByDirectory);

    for (File directory : resultsByFileByDirectory.keySet()) {
      DirectoryResults directoryResult = results.get(directory);
      Collection<FileResults> filesResults = resultsByFileByDirectory.get(directory);
      for (FileResults fileResults : filesResults) {
        directoryResult.addChild(fileResults);
      }
    }
    return results.get(baseDir);
  }

  private static Map<File, DirectoryResults> prepopulateDirectoryResults(File baseDir, Multimap<File, FileResults> resultsByFileByDirectory) {
    Map<File, DirectoryResults> results = Maps.newHashMap();
    results.put(baseDir, new DirectoryResults());
    // add a result by directory for each level from current directory to baseDir
    for (File directory : resultsByFileByDirectory.keySet()) {
      File parent = directory;
      while (!results.containsKey(parent)) {
        results.put(parent, new DirectoryResults());
        parent = parent.getParentFile();
      }
    }

    // construct parent-child relationship between results
    for (Entry<File, DirectoryResults> resultsByFile : results.entrySet()) {
      DirectoryResults directoryResults = resultsByFile.getValue();
      File directory = resultsByFile.getKey();
      for (File subDirectory : directory.listFiles(SUBDIRECTORY_FILTER)) {
        if (results.containsKey(subDirectory)) {
          directoryResults.addChild(results.get(subDirectory));
        }
      }
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

  private static class SubDirectoryFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory();
    }

  }
}
