/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2011 SonarSource
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
package org.sonar.plugins.groovy.codenarc;

import org.apache.commons.lang.StringUtils;

public class RuleParameter {
  String key = "";
  String description = "";
  String defaultValue = "";

  public RuleParameter() {
  }

  public RuleParameter(String key) {
    this.key = key;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RuleParameter other = (RuleParameter) obj;
    if (defaultValue == null) {
      if (other.defaultValue != null) {
        return false;
      }
    } else if (!defaultValue.equals(other.defaultValue)) {
      return false;
    }
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }
    return true;
  }

  public boolean isEmpty() {
    return StringUtils.isBlank(key) && StringUtils.isBlank(defaultValue) && StringUtils.isBlank(description);
  }

  public boolean hasDefaultValue() {
    return StringUtils.isNotBlank(defaultValue);
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void merge(RuleParameter parameter) {
    if (key != null && key.equals(parameter.key)) {
      description = selectValue(description, parameter.description);
      defaultValue = selectValue(defaultValue, parameter.defaultValue);
    }
  }

  private static String selectValue(String currentValue, String newValue) {
    if (StringUtils.isBlank(currentValue) && StringUtils.isNotBlank(newValue)) {
      return newValue;
    }
    return currentValue;
  }

  @Override
  public String toString() {
    String smallDescr = description;
    if (description.length() > 30) {
      smallDescr = description.substring(0, 30) + "...";
    }
    return "RuleParameter [key=" + key + ", defaultValue=" + defaultValue + ", description=" + smallDescr + "]";
  }

}
