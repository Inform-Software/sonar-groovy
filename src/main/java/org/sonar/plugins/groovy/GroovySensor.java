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

package org.sonar.plugins.groovy;

import groovyjarjarantlr.Token;
import groovyjarjarantlr.TokenStream;
import groovyjarjarantlr.TokenStreamException;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import org.gmetrics.GMetricsRunner;
import org.gmetrics.metricset.DefaultMetricSet;
import org.gmetrics.result.MetricResult;
import org.gmetrics.result.NumberMetricResult;
import org.gmetrics.result.SingleNumberMetricResult;
import org.gmetrics.resultsnode.ClassResultsNode;
import org.gmetrics.resultsnode.ResultsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Project;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;
import org.sonar.plugins.groovy.gmetrics.CustomSourceAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.Map.Entry;

public class GroovySensor implements Sensor {

  private static final String CYCLOMATIC_COMPLEXITY_METRIC_NAME = "CyclomaticComplexity";

  private static final Logger LOG = LoggerFactory.getLogger(GroovySensor.class);

  private static final Number[] FUNCTIONS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12};
  private static final Number[] FILES_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};

  private final Settings settings;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final FileSystem fileSystem;

  private double loc = 0;
  private double comments = 0;
  private int currentLine = 0;
  private FileLinesContext fileLinesContext;

  public GroovySensor(Settings settings, FileLinesContextFactory fileLinesContextFactory, FileSystem fileSystem) {
    this.settings = settings;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.fileSystem = fileSystem;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return GroovyFileSystem.hasGroovyFiles(fileSystem);
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    computeBaseMetrics(project, context);
    processFiles(context);
  }

  private void processFiles(SensorContext context) {
    GMetricsRunner runner = new GMetricsRunner();
    runner.setMetricSet(new DefaultMetricSet());
    CustomSourceAnalyzer analyzer = new CustomSourceAnalyzer(fileSystem);
    runner.setSourceAnalyzer(analyzer);
    runner.execute();

    for (Entry<File, Collection<ClassResultsNode>> entry : analyzer.getResultsByFile().asMap().entrySet()) {
      File file = entry.getKey();
      Collection<ClassResultsNode> results = entry.getValue();
      InputFile sonarFile = fileSystem.inputFile(fileSystem.predicates().hasAbsolutePath(file.getAbsolutePath()));
      processFile(context, sonarFile, results);
    }
  }

  private void processFile(SensorContext context, InputFile sonarFile, Collection<ClassResultsNode> results) {
    double classes = 0;
    double methods = 0;
    double complexity = 0;

    RangeDistributionBuilder functionsComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, FUNCTIONS_DISTRIB_BOTTOM_LIMITS);

    for (ClassResultsNode result : results) {
      classes += 1;

      for (ResultsNode resultsNode : result.getChildren().values()) {
        methods += 1;
        for (MetricResult metricResult : resultsNode.getMetricResults()) {
          String metricName = metricResult.getMetric().getName();
          if (CYCLOMATIC_COMPLEXITY_METRIC_NAME.equals(metricName)) {
            int value = (Integer) ((SingleNumberMetricResult) metricResult).getNumber();
            functionsComplexityDistribution.add(value);
          }
        }
      }

      for (MetricResult metricResult : result.getMetricResults()) {
        String metricName = metricResult.getMetric().getName();
        if (CYCLOMATIC_COMPLEXITY_METRIC_NAME.equals(metricName)) {
          int value = (Integer) ((NumberMetricResult) metricResult).getValues().get("total");
          complexity += value;
        }
      }
    }

    context.saveMeasure(sonarFile, CoreMetrics.FILES, 1.0);
    context.saveMeasure(sonarFile, CoreMetrics.CLASSES, classes);
    context.saveMeasure(sonarFile, CoreMetrics.FUNCTIONS, methods);
    context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, complexity);

    context.saveMeasure(sonarFile, functionsComplexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
    RangeDistributionBuilder fileComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, FILES_DISTRIB_BOTTOM_LIMITS);
    fileComplexityDistribution.add(complexity);
    context.saveMeasure(sonarFile, fileComplexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
  }

  private void computeBaseMetrics(Project project, SensorContext sensorContext) {
    for (File groovyFile : GroovyFileSystem.sourceFiles(fileSystem)) {
      InputFile resource = fileSystem.inputFile(fileSystem.predicates().hasAbsolutePath(groovyFile.getAbsolutePath()));
      if (resource != null) {
        loc = 0;
        comments = 0;
        currentLine = 0;
        fileLinesContext = fileLinesContextFactory.createFor(resource);
        try {
          GroovyLexer groovyLexer = new GroovyLexer(new FileReader(groovyFile));
          groovyLexer.setWhitespaceIncluded(true);
          TokenStream tokenStream = groovyLexer.plumb();
          Token token = tokenStream.nextToken();
          Token nextToken = tokenStream.nextToken();
          while (nextToken.getType() != Token.EOF_TYPE) {
            handleToken(token, nextToken.getLine());
            token = nextToken;
            nextToken = tokenStream.nextToken();
          }
          handleToken(token, nextToken.getLine());
          sensorContext.saveMeasure(resource, CoreMetrics.LINES, (double) nextToken.getLine());
          sensorContext.saveMeasure(resource, CoreMetrics.NCLOC, loc);
          sensorContext.saveMeasure(resource, CoreMetrics.COMMENT_LINES, comments);
        } catch (TokenStreamException tse) {
          LOG.error("Unexpected token when lexing file : " + groovyFile.getName(), tse);
        } catch (FileNotFoundException fnfe) {
          LOG.error("Could not find : " + groovyFile.getName(), fnfe);
        }
        fileLinesContext.save();
      }
    }
  }

  private void handleToken(Token token, int nextTokenLine) {
    int tokenType = token.getType();
    int tokenLine = token.getLine();
    if (isComment(tokenType)) {
      if (isNotHeaderComment(tokenLine)) {
        comments += nextTokenLine - tokenLine + 1;
      }
      for (int commentLineNb = tokenLine; commentLineNb <= nextTokenLine; commentLineNb++) {
        fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, commentLineNb, 1);
      }
    } else if (isNotWhitespace(tokenType) && tokenLine != currentLine) {
      loc++;
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, tokenLine, 1);
      currentLine = tokenLine;
    }
  }

  private boolean isNotHeaderComment(int tokenLine) {
    return !(tokenLine == 1 && settings.getBoolean(GroovyPlugin.IGNORE_HEADER_COMMENTS));
  }

  private boolean isNotWhitespace(int tokenType) {
    return !(tokenType == GroovyTokenTypes.WS ||
      tokenType == GroovyTokenTypes.STRING_NL ||
      tokenType == GroovyTokenTypes.ONE_NL || tokenType == GroovyTokenTypes.NLS);
  }

  private boolean isComment(int tokenType) {
    return tokenType == GroovyTokenTypes.SL_COMMENT || tokenType == GroovyTokenTypes.SH_COMMENT || tokenType == GroovyTokenTypes.ML_COMMENT;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
