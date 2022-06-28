/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.lang.impl;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

class TokenSequence {
    private static final Logger LOG = Logger.getInstance("#com.intellij.lang.impl.TokenSequence");

    final int[] lexStarts;
    final IElementType[] lexTypes;
    final int lexemeCount;

    TokenSequence(int[] lexStarts, IElementType[] lexTypes, int lexemeCount) {
        this.lexStarts = lexStarts;
        this.lexTypes = lexTypes;
        this.lexemeCount = lexemeCount;
        assert lexemeCount < lexStarts.length;
        assert lexemeCount < lexTypes.length;
    }

    void assertMatches(@NotNull CharSequence text, @NotNull Lexer lexer) {
        TokenSequence sequence = new Builder(text, lexer).performLexing();
        assert lexemeCount == sequence.lexemeCount;
        for (int j = 0; j <= lexemeCount; ++j) {
            if (sequence.lexStarts[j] != lexStarts[j] || sequence.lexTypes[j] != lexTypes[j]) {
                assert false;
            }
        }
    }

    static class Builder {
        private final CharSequence myText;
        private final Lexer myLexer;
        private int[] myLexStarts;
        private IElementType[] myLexTypes;

        Builder(@NotNull CharSequence text, @NotNull Lexer lexer) {
            myText = text;
            myLexer = lexer;

            int approxLexCount = Math.max(10, myText.length() / 5);

            myLexStarts = new int[approxLexCount];
            myLexTypes = new IElementType[approxLexCount];
        }

        @NotNull
        TokenSequence performLexing() {
            myLexer.start(myText);
            int i = 0;
            int offset = 0;
            while (true) {
                IElementType type = myLexer.getTokenType();
                if (type == null) break;

                if (i % 20 == 0) ProgressIndicatorProvider.checkCanceled();

                if (i >= myLexTypes.length - 1) {
                    resizeLexemes(i * 3 / 2);
                }
                int tokenStart = myLexer.getTokenStart();
                if (tokenStart < offset) {
                    reportDescendingOffsets(i, offset, tokenStart);
                }
                myLexStarts[i] = offset = tokenStart;
                myLexTypes[i] = type;
                i++;
                myLexer.advance();
            }

            myLexStarts[i] = myText.length();

            return new TokenSequence(myLexStarts, myLexTypes, i);
        }

        private void reportDescendingOffsets(int tokenIndex, int offset, int tokenStart) {
            StringBuilder sb = new StringBuilder();
            IElementType tokenType = myLexer.getTokenType();
            sb.append("Token sequence broken")
                    .append("\n  this: '").append(myLexer.getTokenText()).append("' (").append(tokenType).append(':')
                    .append(tokenType != null ? tokenType.getLanguage() : null).append(") ").append(tokenStart).append(":")
                    .append(myLexer.getTokenEnd());
            if (tokenIndex > 0) {
                int prevStart = myLexStarts[tokenIndex - 1];
                sb.append("\n  prev: '").append(myText.subSequence(prevStart, offset)).append("' (").append(myLexTypes[tokenIndex - 1]).append(':')
                        .append(myLexTypes[tokenIndex - 1].getLanguage()).append(") ").append(prevStart).append(":").append(offset);
            }
            int quoteStart = Math.max(tokenStart - 256, 0);
            int quoteEnd = Math.min(tokenStart + 256, myText.length());
            sb.append("\n  quote: [").append(quoteStart).append(':').append(quoteEnd)
                    .append("] '").append(myText.subSequence(quoteStart, quoteEnd)).append('\'');
            LOG.error(sb);
        }

        private void resizeLexemes(final int newSize) {
            myLexStarts = ArrayUtil.realloc(myLexStarts, newSize);
            myLexTypes = ArrayUtil.realloc(myLexTypes, newSize, IElementType.ARRAY_FACTORY);
        }

    }
}
