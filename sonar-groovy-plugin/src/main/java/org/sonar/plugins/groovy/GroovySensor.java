/*
 * Sonar Groovy Plugin
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
package org.sonar.plugins.groovy;

import groovyjarjarantlr.Token;
import groovyjarjarantlr.TokenStream;
import groovyjarjarantlr.TokenStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.antlr.GroovySourceToken;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import org.gmetrics.result.MetricResult;
import org.gmetrics.result.MutableMapMetricResult;
import org.gmetrics.result.NumberMetricResult;
import org.gmetrics.result.SingleNumberMetricResult;
import org.gmetrics.resultsnode.ClassResultsNode;
import org.gmetrics.resultsnode.PackageResultsNode;
import org.gmetrics.resultsnode.ResultsNode;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.ce.measure.RangeDistributionBuilder;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.groovy.foundation.Groovy;
import org.sonar.plugins.groovy.foundation.GroovyFileSystem;
import org.sonar.plugins.groovy.foundation.GroovyHighlighterAndTokenizer;
import org.sonar.plugins.groovy.gmetrics.GMetricsSourceAnalyzer;

public class GroovySensor implements Sensor {
  private static final Logger LOG = Loggers.get(GroovySensor.class);

  private static final String CYCLOMATIC_COMPLEXITY_METRIC_NAME = "CyclomaticComplexity";
  private static final String EFFERENT_COUPLING_METRIC_NAME = "EfferentCoupling";
  private static final String AFFERENT_COUPLING_METRIC_NAME = "AfferentCoupling";

  private static final Number[] FUNCTIONS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12};
  private static final Number[] FILES_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};

  private static final Set<String> EMPTY_COMMENT_LINES = Arrays.stream(new String[] {"/**", "/*", "*", "*/", "//"}).collect(Collectors.toSet());

  private final Settings settings;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final GroovyFileSystem groovyFileSystem;

  private int loc = 0;
  private int comments = 0;
  private int currentLine = 0;
  private FileLinesContext fileLinesContext;

  public GroovySensor(Settings settings, FileLinesContextFactory fileLinesContextFactory, FileSystem fileSystem) {
    this.settings = settings;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.groovyFileSystem = new GroovyFileSystem(fileSystem);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Groovy.KEY).name(this.toString());
  }

  @Override
  public void execute(SensorContext context) {
    if (groovyFileSystem.hasGroovyFiles()) {
      List<InputFile> inputFiles = groovyFileSystem.sourceInputFiles();
      computeBaseMetrics(context, inputFiles);
      computeGroovyMetrics(context, inputFiles);
      highlightFiles(context, groovyFileSystem.groovyInputFiles());
    }
  }

  private static void computeGroovyMetrics(SensorContext context, List<InputFile> inputFiles) {
    GMetricsSourceAnalyzer metricsAnalyzer = new GMetricsSourceAnalyzer(context.fileSystem(), inputFiles);

    metricsAnalyzer.analyze();

    for (Entry<InputFile, List<ClassResultsNode>> entry : metricsAnalyzer.resultsByFile().entrySet()) {
      processFile(context, entry.getKey(), entry.getValue());
    }

    for (Entry<InputDir, PackageResultsNode> entry : metricsAnalyzer.resultsByPackage().entrySet()) {
      processPackage(context, entry.getKey(), entry.getValue().getMetricResults());
    }
  }

  private static void processFile(SensorContext context, InputFile sonarFile, Collection<ClassResultsNode> results) {
    int classes = 0;
    int methods = 0;
    int complexity = 0;
    int complexityInFunctions = 0;

    RangeDistributionBuilder functionsComplexityDistribution = new RangeDistributionBuilder(FUNCTIONS_DISTRIB_BOTTOM_LIMITS);

    for (ClassResultsNode result : results) {
      classes += 1;

      for (ResultsNode resultsNode : result.getChildren().values()) {
        methods += 1;
        Optional<MetricResult> cyclomaticComplexity = getCyclomaticComplexity(resultsNode.getMetricResults());
        if (cyclomaticComplexity.isPresent()) {
          int value = (Integer) ((SingleNumberMetricResult) cyclomaticComplexity.get()).getNumber();
          functionsComplexityDistribution.add(value);
          complexityInFunctions += value;
        }
      }

      Optional<MetricResult> cyclomaticComplexity = getCyclomaticComplexity(result.getMetricResults());
      if (cyclomaticComplexity.isPresent()) {
        int value = (Integer) ((NumberMetricResult) cyclomaticComplexity.get()).getValues().get("total");
        complexity += value;
      }
    }

    saveMetric(context, sonarFile, CoreMetrics.FILES, 1);
    saveMetric(context, sonarFile, CoreMetrics.CLASSES, classes);
    saveMetric(context, sonarFile, CoreMetrics.FUNCTIONS, methods);
    saveMetric(context, sonarFile, CoreMetrics.COMPLEXITY, complexity);
    saveMetric(context, sonarFile, CoreMetrics.COMPLEXITY_IN_CLASSES, complexity);
    saveMetric(context, sonarFile, CoreMetrics.COMPLEXITY_IN_FUNCTIONS, complexityInFunctions);
    saveMetric(context, sonarFile, CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, functionsComplexityDistribution.build());

    RangeDistributionBuilder fileComplexityDistribution = new RangeDistributionBuilder(FILES_DISTRIB_BOTTOM_LIMITS);
    fileComplexityDistribution.add(complexity);
    saveMetric(context, sonarFile, CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, fileComplexityDistribution.build());
  }

  private static Optional<MetricResult> getCyclomaticComplexity(List<MetricResult> metricResults) {
    return metricResults
      .stream()
      .filter(metricResult -> CYCLOMATIC_COMPLEXITY_METRIC_NAME.equals(metricResult.getMetric().getName()))
      .findAny();
  }

  private static void processPackage(SensorContext context, InputDir inputDir, List<MetricResult> metricResults) {
    for (MetricResult metricResult : metricResults) {
      org.gmetrics.metric.Metric metric = metricResult.getMetric();
      String metricName = metric.getName();
      if (EFFERENT_COUPLING_METRIC_NAME.equals(metricName)) {
        MutableMapMetricResult result = (MutableMapMetricResult) metricResult;
        saveMetric(context, inputDir, GroovyMetrics.EFFERENT_COUPLING_TOTAL, getTotalValue(result));
        saveMetric(context, inputDir, GroovyMetrics.EFFERENT_COUPLING_AVERAGE, getAverageValue(result));
      } else if (AFFERENT_COUPLING_METRIC_NAME.equals(metricName)) {
        MutableMapMetricResult result = (MutableMapMetricResult) metricResult;
        saveMetric(context, inputDir, GroovyMetrics.AFFERENT_COUPLING_TOTAL, getTotalValue(result));
        saveMetric(context, inputDir, GroovyMetrics.AFFERENT_COUPLING_AVERAGE, getAverageValue(result));
      }
    }
  }

  private static Integer getTotalValue(MutableMapMetricResult result) {
    return (Integer) result.getAt("total");
  }

  private static double getAverageValue(MutableMapMetricResult result) {
    Object avg = result.getAt("average");
    BigDecimal avgValue = (avg instanceof Integer) ? new BigDecimal((Integer) avg) : (BigDecimal) avg;
    return avgValue.doubleValue();
  }

  private void computeBaseMetrics(SensorContext context, List<InputFile> inputFiles) {
    for (InputFile groovyFile : inputFiles) {
      computeBaseMetrics(context, groovyFile);
    }
  }

  private void computeBaseMetrics(SensorContext context, InputFile groovyFile) {
    File file = groovyFile.file();
    if (file.exists()) {
      loc = 0;
      comments = 0;
      currentLine = 0;
      fileLinesContext = fileLinesContextFactory.createFor(groovyFile);
      Charset encoding = context.fileSystem().encoding();
      try (InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file), encoding)) {
        List<String> lines = FileUtils.readLines(file, encoding);
        GroovyLexer groovyLexer = new GroovyLexer(streamReader);
        groovyLexer.setWhitespaceIncluded(true);
        TokenStream tokenStream = groovyLexer.plumb();
        Token token = tokenStream.nextToken();
        Token nextToken = tokenStream.nextToken();
        while (nextToken.getType() != Token.EOF_TYPE) {
          handleToken(token, nextToken.getLine(), lines);
          token = nextToken;
          nextToken = tokenStream.nextToken();
        }
        handleToken(token, nextToken.getLine(), lines);
        saveMetric(context, groovyFile, CoreMetrics.LINES, nextToken.getLine());
        saveMetric(context, groovyFile, CoreMetrics.NCLOC, loc);
        saveMetric(context, groovyFile, CoreMetrics.COMMENT_LINES, comments);
      } catch (TokenStreamException e) {
        LOG.error("Unexpected token when lexing file : " + file.getName(), e);
      } catch (IOException e) {
        LOG.error("Unable to read file: " + file.getName(), e);
      }
      fileLinesContext.save();
    }
  }

  private static void highlightFiles(SensorContext context, List<InputFile> inputFiles) {
    for (InputFile inputFile : inputFiles) {
      new GroovyHighlighterAndTokenizer(inputFile).processFile(context);
    }
  }

  private static <T extends Serializable> void saveMetric(SensorContext context, InputComponent inputComponent, Metric<T> metric, T value) {
    context.<T>newMeasure()
      .withValue(value)
      .forMetric(metric)
      .on(inputComponent)
      .save();
  }

  private void handleToken(Token token, int nextTokenLine, List<String> lines) {
    int tokenType = token.getType();
    int tokenLine = token.getLine();
    if (isComment(tokenType)) {
      if (isNotHeaderComment(tokenLine)) {
        comments += nextTokenLine - tokenLine + 1 - numberEmptyLines(token, lines);
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

  private int numberEmptyLines(Token token, List<String> lines) {
    List<String> relatedLines = getLinesFromToken(lines, (GroovySourceToken) token);
    long emptyLines = relatedLines.stream().map(String::trim).filter(EMPTY_COMMENT_LINES::contains).count();
    return (int) emptyLines;
  }

  private static List<String> getLinesFromToken(List<String> lines, GroovySourceToken gst) {
    List<String> newLines = new ArrayList<>(lines.subList(gst.getLine() - 1, gst.getLineLast()));

    int lastLineIndex = newLines.size() - 1;
    String lastLine = newLines.get(lastLineIndex).substring(0, gst.getColumnLast() - 1);
    newLines.set(lastLineIndex, lastLine);

    String firstLine = newLines.get(0).substring(gst.getColumn() - 1);
    newLines.set(0, firstLine);

    return newLines;
  }

  private boolean isNotHeaderComment(int tokenLine) {
    return !(tokenLine == 1 && settings.getBoolean(GroovyPlugin.IGNORE_HEADER_COMMENTS));
  }

  private static boolean isNotWhitespace(int tokenType) {
    return !(tokenType == GroovyTokenTypes.WS ||
      tokenType == GroovyTokenTypes.STRING_NL ||
      tokenType == GroovyTokenTypes.ONE_NL || tokenType == GroovyTokenTypes.NLS);
  }

  private static boolean isComment(int tokenType) {
    return tokenType == GroovyTokenTypes.SL_COMMENT || tokenType == GroovyTokenTypes.SH_COMMENT || tokenType == GroovyTokenTypes.ML_COMMENT;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
