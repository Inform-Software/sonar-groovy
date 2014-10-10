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

package org.sonar.plugins.groovy.foundation;

import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

public class Groovy extends AbstractLanguage {

  public static final String KEY = "grvy";

  public Groovy() {
    super(KEY, "Groovy");
  }

  @Override
  public String[] getFileSuffixes() {
    return new String[] {"groovy"};
  }

  public static boolean isEnabled(ModuleFileSystem moduleFileSystem) {
    return !moduleFileSystem.files(FileQuery.onSource().onLanguage(KEY)).isEmpty();
  }

}
