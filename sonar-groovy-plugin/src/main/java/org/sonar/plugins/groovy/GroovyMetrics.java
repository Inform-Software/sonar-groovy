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
package org.sonar.plugins.groovy;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.Metrics;

public final class GroovyMetrics implements Metrics {

  public static final Metric<Double> EFFERENT_COUPLING_AVERAGE = new Metric.Builder(
    "efferent_coupling_average",
    "Efferent Coupling (Average)",
    ValueType.FLOAT)
      .setDescription(
        "Shows the Efferent Coupling for a package. "
          + "This is a count of the number of other packages that the classes in a package depend upon, "
          + "and is an indicator of the package's independence.")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
      .create();

  public static final Metric<Integer> EFFERENT_COUPLING_TOTAL = new Metric.Builder(
    "efferent_coupling_total",
    "Efferent Coupling (Total)",
    ValueType.INT)
      .setDescription(
        "Shows the Efferent Coupling for a package. "
          + "This is a count of the number of other packages that the classes in a package depend upon, "
          + "and is an indicator of the package's independence.")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
      .create();

  public static final Metric<Double> AFFERENT_COUPLING_AVERAGE = new Metric.Builder(
    "afferent_coupling_average",
    "Afferent Coupling (Average)",
    ValueType.FLOAT)
      .setDescription(
        "Shows the Afferent Coupling for a package. "
          + "This is a count of the number of other packages that depend on the classes within this package. "
          + "It is an indicator of the package's responsibility.")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
      .create();

  public static final Metric<Integer> AFFERENT_COUPLING_TOTAL = new Metric.Builder(
    "afferent_coupling_total",
    "Afferent Coupling (Total)",
    ValueType.INT)
      .setDescription(
        "Shows the Afferent Coupling for a package. "
          + "This is a count of the number of other packages that depend on the classes within this package. "
          + "It is an indicator of the package's responsibility.")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
      .create();

  @Override
  public List<Metric> getMetrics() {
    return Arrays.asList(
      EFFERENT_COUPLING_AVERAGE,
      EFFERENT_COUPLING_TOTAL,
      AFFERENT_COUPLING_AVERAGE,
      AFFERENT_COUPLING_TOTAL);
  }
}
