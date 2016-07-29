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
package org.sonar.plugins.groovy.gmetrics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.gmetrics.analyzer.SourceAnalyzer;
import org.gmetrics.metric.Metric;
import org.gmetrics.metricset.MetricSet;
import org.gmetrics.result.ClassMetricResult;
import org.gmetrics.resultsnode.ClassResultsNode;
import org.gmetrics.resultsnode.PackageResultsNode;
import org.gmetrics.resultsnode.ResultsNode;
import org.gmetrics.source.SourceCode;
import org.gmetrics.source.SourceFile;
import org.sonar.api.batch.fs.InputFile;

import java.util.Collections;
import java.util.List;

/**
 * Custom implementation of {@link org.gmetrics.analyzer.FilesystemSourceAnalyzer}.
 */
public class CustomSourceAnalyzer implements SourceAnalyzer {

  private final Multimap<InputFile, ClassResultsNode> resultsByFile = ArrayListMultimap.create();
  private final String baseDirAbsolutePath;
  private final List<InputFile> sourceFiles;

  public CustomSourceAnalyzer(String baseDirAbsolutePath, List<InputFile> sourceFiles) {
    this.baseDirAbsolutePath = baseDirAbsolutePath;
    this.sourceFiles = sourceFiles;
  }

  public Multimap<InputFile, ClassResultsNode> getResultsByFile() {
    return resultsByFile;
  }

  @Override
  public List<String> getSourceDirectories() {
    return Collections.singletonList(baseDirAbsolutePath);
  }

  @Override
  public ResultsNode analyze(MetricSet metricSet) {
    return processFiles(metricSet);
  }

  private PackageResultsNode processFiles(MetricSet metricSet) {
    for (InputFile file : sourceFiles) {
      SourceCode sourceCode = new SourceFile(file.file());
      ModuleNode ast = sourceCode.getAst();
      if (ast != null) {
        for (ClassNode classNode : ast.getClasses()) {
          String className = classNode.getName();
          ClassResultsNode classResults = new ClassResultsNode(className);
          for (Object metric : metricSet.getMetrics()) {
            ClassMetricResult classMetricResult = ((Metric) metric).applyToClass(classNode, sourceCode);
            classResults.addClassMetricResult(classMetricResult);
          }
          resultsByFile.put(file, classResults);
        }
      }
    }
    // Only file results are used
    return null;
  }
}
