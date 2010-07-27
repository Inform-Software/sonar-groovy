/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.plugins.groovy.foundation;

import net.sourceforge.pmd.cpd.Tokenizer;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.resources.*;

import java.io.File;
import java.util.List;

public class GroovyCpdMapping implements CpdMapping {

  public Tokenizer getTokenizer() {
    return new GroovyCpdTokenizer();
  }

  public Resource createResource(File file, List<File> sourceDirs) {
    return GroovyFile.fromIOFile(file, sourceDirs);
  }

  public Language getLanguage() {
    return Groovy.INSTANCE;
  }

}
