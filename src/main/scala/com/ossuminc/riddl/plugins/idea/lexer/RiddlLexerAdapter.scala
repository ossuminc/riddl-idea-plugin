/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.ossuminc.riddl.language.AST.Token
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.utils.{NullLogger, pc}

/** A Lexer adapter that wraps RIDDL's token parser for IntelliJ.
  *
  * This lexer uses RIDDL's `TopLevelParser.parseToTokens()` to tokenize
  * the input, then presents those tokens through IntelliJ's Lexer interface.
  *
  * Design decision: We wrap RIDDL's tokenization rather than implementing
  * a separate lexer to:
  * - Maintain a single source of truth for RIDDL tokenization
  * - Ensure consistency with other RIDDL tools (VS Code, etc.)
  * - Automatically benefit from RIDDL grammar updates
  *
  * Trade-off: We lose IntelliJ's mid-document restart optimization, but
  * RIDDL's structural dependencies prevent partial parsing anyway.
  */
class RiddlLexerAdapter extends LexerBase {

  private var buffer: CharSequence = ""
  private var bufferEnd: Int = 0
  private var tokens: Array[Token] = Array.empty
  private var tokenIndex: Int = 0

  override def start(
      buffer: CharSequence,
      startOffset: Int,
      endOffset: Int,
      initialState: Int
  ): Unit = {
    this.buffer = buffer
    this.bufferEnd = endOffset

    // Parse tokens from the buffer
    val text = buffer.subSequence(startOffset, endOffset).toString
    this.tokens = parseTokens(text, startOffset)
    this.tokenIndex = 0
  }

  private def parseTokens(text: String, baseOffset: Int): Array[Token] =
    if text.isEmpty then Array.empty
    else
      pc.withLogger(NullLogger()) { _ =>
        val rpi = RiddlParserInput(text, "editor")
        TopLevelParser.parseToTokens(rpi, withVerboseFailures = false) match
          case Right(tokenSeq) => tokenSeq.toArray
          case Left(_) =>
            // On parse failure, return a single "Other" token for the whole text
            Array(Token.Other(com.ossuminc.riddl.language.At(0, text.length, rpi)))
      }

  override def getState: Int = 0 // Stateless lexer

  override def getTokenType: IElementType | Null =
    if tokenIndex >= tokens.length then null
    else RiddlTokenTypes.fromRiddlToken(tokens(tokenIndex))

  override def getTokenStart: Int =
    if tokenIndex >= tokens.length then bufferEnd
    else tokens(tokenIndex).loc.offset

  override def getTokenEnd: Int =
    if tokenIndex >= tokens.length then bufferEnd
    else tokens(tokenIndex).loc.endOffset

  override def advance(): Unit = {
    tokenIndex += 1
  }

  override def getBufferSequence: CharSequence = buffer

  override def getBufferEnd: Int = bufferEnd
}
