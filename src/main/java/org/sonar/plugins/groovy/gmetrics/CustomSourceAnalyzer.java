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

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Custom implementation of {@link org.gmetrics.analyzer.FilesystemSourceAnalyzer}.
 */
public class CustomSourceAnalyzer implements SourceAnalyzer {

  private final String baseDirectory;
  private final Multimap<File, ClassResultsNode> resultsByFile = ArrayListMultimap.create();

  public CustomSourceAnalyzer(String baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  public Multimap<File, ClassResultsNode> getResultsByFile() {
    return resultsByFile;
  }

  public List<String> getSourceDirectories() {
    return Collections.singletonList(baseDirectory);
  }

  public ResultsNode analyze(MetricSet metricSet) {
    File dir = new File(baseDirectory);
    return processDirectory(dir, "", metricSet);
  }

  private PackageResultsNode processDirectory(File dir, String path, MetricSet metricSet) {
    String packageName = path.length() == 0 ? "" : dir.getName();
    PackageResultsNode packageResults = new PackageResultsNode(packageName, packageName, path);
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        String filePath = path + "/" + file.getName();
        PackageResultsNode subpackageResults = processDirectory(file, filePath, metricSet);
        if (subpackageResults.containsClassResults()) {
          packageResults.addChildIfNotEmpty(filePath, subpackageResults);
        }
      } else {
        processFile(file, packageResults, metricSet);
      }
    }
    for (Object metric : metricSet.getMetrics()) {
      packageResults.applyMetric((Metric) metric);
    }
    return packageResults;
  }

  private void processFile(File file, PackageResultsNode packageResults, MetricSet metricSet) {
    if (!file.getPath().endsWith(".groovy")) {
      return;
    }
    SourceCode sourceCode = new SourceFile(file);
    ModuleNode ast = sourceCode.getAst();
    if (ast != null) {
      for (ClassNode classNode : ast.getClasses()) {
        String className = classNode.getName();
        ClassResultsNode classResults = new ClassResultsNode(className);
        for (Object metric : metricSet.getMetrics()) {
          ClassMetricResult classMetricResult = ((Metric) metric).applyToClass(classNode, sourceCode);
          classResults.addClassMetricResult(classMetricResult);
        }
        packageResults.addChildIfNotEmpty(className, classResults);
        resultsByFile.put(file, classResults);
      }
    }
  }

}
