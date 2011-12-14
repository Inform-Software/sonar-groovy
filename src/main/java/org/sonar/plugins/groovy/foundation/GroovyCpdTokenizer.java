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

import groovyjarjarantlr.Token;
import groovyjarjarantlr.TokenStream;
import groovyjarjarantlr.TokenStreamException;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.sonar.plugins.groovy.utils.GroovyUtils;

import java.io.*;

public class GroovyCpdTokenizer implements Tokenizer {
  public final void tokenize(SourceCode source, Tokens cpdTokens) {
    String fileName = source.getFileName();
    Token token;
    TokenStream tokenStream;

    try {
      tokenStream = new GroovyLexer(new FileReader(new File(fileName))).plumb();
      token = tokenStream.nextToken();
      while (token.getType() != Token.EOF_TYPE) {
        cpdTokens.add(new TokenEntry(token.getText(), fileName, token.getLine()));
        token = tokenStream.nextToken();
      }
    }
    catch (TokenStreamException tse) {
      GroovyUtils.LOG.error("Unexpected token when lexing file : " + fileName, tse);
    }
    catch (FileNotFoundException fnfe) {
      GroovyUtils.LOG.error("Could not find : " + fileName, fnfe);
    }
    cpdTokens.add(TokenEntry.getEOF());
  }
}
