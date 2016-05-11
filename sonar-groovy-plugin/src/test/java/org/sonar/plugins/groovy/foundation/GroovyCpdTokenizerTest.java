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
package org.sonar.plugins.groovy.foundation;

import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokens;
import org.junit.Test;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroovyCpdTokenizerTest {

  /**
   * See SONARPLUGINS-596
   */
  @Test
  public void should_lex() {
    SourceCode source = mock(SourceCode.class);
    File file = TestUtils.getResource("/org/sonar/plugins/groovy/foundation/Greet.groovy");
    when(source.getFileName()).thenReturn(file.getAbsolutePath());
    Tokens cpdTokens = new Tokens();
    new GroovyCpdTokenizer().tokenize(source, cpdTokens);
    List<TokenEntry> tokens = cpdTokens.getTokens();

    assertThat(tokens.size()).isEqualTo(31);
    assertThat(tokens.get(tokens.size() - 1)).isSameAs(TokenEntry.getEOF());
  }

  @Test
  public void should_tokens_only_contains_EOF_if_file_does_not_exist() {
    SourceCode source = mock(SourceCode.class);
    File file = new File("/org/sonar/plugins/groovy/foundation/Greet-fake.groovy");
    when(source.getFileName()).thenReturn(file.getAbsolutePath());
    Tokens cpdTokens = new Tokens();
    new GroovyCpdTokenizer().tokenize(source, cpdTokens);
    List<TokenEntry> tokens = cpdTokens.getTokens();

    assertThat(tokens.size()).isEqualTo(1);
    assertThat(tokens.get(tokens.size() - 1)).isSameAs(TokenEntry.getEOF());
  }

  @Test
  public void should_lex_stop_when_encountering_unexpected_token() {
    SourceCode source = mock(SourceCode.class);
    File file = TestUtils.getResource("/org/sonar/plugins/groovy/foundation/Error.groovy");
    when(source.getFileName()).thenReturn(file.getAbsolutePath());
    Tokens cpdTokens = new Tokens();
    new GroovyCpdTokenizer().tokenize(source, cpdTokens);
    List<TokenEntry> tokens = cpdTokens.getTokens();

    assertThat(tokens.size()).isEqualTo(7);
    assertThat(tokens.get(tokens.size() - 1)).isSameAs(TokenEntry.getEOF());
    assertThat(tokens.get(tokens.size() - 2).getValue()).isEqualTo("name");
  }

}
