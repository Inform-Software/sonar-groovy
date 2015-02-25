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

import com.google.common.collect.Lists;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile.Type;

import java.io.File;
import java.util.List;

public class GroovyFileSystem {

  private GroovyFileSystem() {
  }

  public static boolean hasGroovyFiles(FileSystem fileSystem) {
    return fileSystem.hasFiles(fileSystem.predicates().hasLanguage(Groovy.KEY));
  }

  public static List<File> sourceFiles(FileSystem fileSystem) {
    FilePredicates predicates = fileSystem.predicates();
    Iterable<File> files = fileSystem.files(predicates.and(predicates.hasLanguage(Groovy.KEY), predicates.hasType(Type.MAIN)));
    return Lists.<File>newArrayList(files);
  }

}
