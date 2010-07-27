/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.groovy.gmetrics;

import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.groovy.foundation.GroovyFile;
import org.sonar.plugins.groovy.utils.GroovyUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;

public class GMetricsXMLParser {

  private final static Number[] METHODS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12};
  private final static Number[] CLASS_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};

  public void parseAndProcessGMetricsResults(File xmlFile, final SensorContext context) {
    GroovyUtils.LOG.info("parsing {}", xmlFile);

    StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        SMInputCursor clazz = rootCursor.descendantElementCursor("Class");

        while (clazz.getNext() != null) {
          GroovyFile sonarFile = new GroovyFile(clazz.getAttrValue("name"));

          double nbMethods = 0;
          double complexity = 0;

          RangeDistributionBuilder complexityMethodsDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION,
            METHODS_DISTRIB_BOTTOM_LIMITS);
          RangeDistributionBuilder complexityClassDistribution = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION,
            CLASS_DISTRIB_BOTTOM_LIMITS);

          SMInputCursor method = clazz.descendantElementCursor("Method");

          while (method.getNext() != null) {
            nbMethods++;
            SMInputCursor methodMetric = method.childElementCursor("MetricResult");
            while (methodMetric.getNext() != null) {
              if (methodMetric.getAttrValue("name").equals("CyclomaticComplexity")) {
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
    });
    try {
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      GroovyUtils.LOG.error("Error parsing file : " + xmlFile);
    }
  }
}
