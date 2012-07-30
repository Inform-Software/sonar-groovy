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

import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;

import java.io.File;

public class GMetricsXMLParser implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(GMetricsXMLParser.class);

  private static final Number[] METHODS_DISTRIB_BOTTOM_LIMITS = { 1, 2, 4, 6, 8, 10, 12 };
  private static final Number[] CLASS_DISTRIB_BOTTOM_LIMITS = { 0, 5, 10, 20, 30, 60, 90 };

  public void parseAndProcessGMetricsResults(File xmlFile, final SensorContext context) {
    LOG.info("parsing {}", xmlFile);

    StaxParser parser = new StaxParser(new GMetricsStreamHandler(context));
    try {
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      LOG.error("Error parsing file : " + xmlFile);
    }
  }

  private static class GMetricsStreamHandler implements StaxParser.XmlStreamHandler {
    private final SensorContext context;

    public GMetricsStreamHandler(SensorContext context) {
      this.context = context;
    }

    public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
      rootCursor.advance();
      SMInputCursor clazz = rootCursor.descendantElementCursor("Class");

      while (clazz.getNext() != null) {
        String classKey = clazz.getAttrValue("name");
        // TODO we assume that there is one file per class
        String filename = classKey.replace('.', '/') + ".groovy";
        org.sonar.api.resources.File sonarFile = new org.sonar.api.resources.File(filename);

        double nbMethods = 0;
        double complexity = 0;

        RangeDistributionBuilder complexityMethodsDistribution =
            new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, METHODS_DISTRIB_BOTTOM_LIMITS);
        RangeDistributionBuilder complexityClassDistribution =
            new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, CLASS_DISTRIB_BOTTOM_LIMITS);

        SMInputCursor method = clazz.descendantElementCursor("Method");

        while (method.getNext() != null) {
          nbMethods++;
          SMInputCursor methodMetric = method.childElementCursor("MetricResult");
          while (methodMetric.getNext() != null) {
            if ("CyclomaticComplexity".equals(methodMetric.getAttrValue("name"))) {
              double methodComplexity = Double.valueOf(methodMetric.getAttrValue("total"));
              complexity += methodComplexity;
              complexityMethodsDistribution.add(methodComplexity);
            }
          }
        }
        context.saveMeasure(sonarFile, CoreMetrics.FUNCTIONS, nbMethods);
        context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, complexity);
        context.saveMeasure(sonarFile, complexityMethodsDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));

        complexityClassDistribution.add(complexity);
        context.saveMeasure(sonarFile, complexityClassDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
      }
    }
  }

}
