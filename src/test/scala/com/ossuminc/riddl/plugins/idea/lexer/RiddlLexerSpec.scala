/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.lexer

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL lexer adapter.
  *
  * These tests verify that the RiddlLexerAdapter correctly wraps RIDDL's
  * tokenization and produces the expected IntelliJ token types.
  */
class RiddlLexerSpec extends AnyWordSpec with Matchers {

  "RiddlLexerAdapter" must {

    "tokenize an empty string" in {
      val lexer = new RiddlLexerAdapter()
      lexer.start("", 0, 0, 0)

      lexer.getTokenType mustBe null
    }

    "tokenize a simple keyword" in {
      val lexer = new RiddlLexerAdapter()
      val text = "domain"
      lexer.start(text, 0, text.length, 0)

      lexer.getTokenType mustBe RiddlTokenTypes.KEYWORD
      lexer.getTokenStart mustBe 0
      lexer.getTokenEnd mustBe 6

      lexer.advance()
      lexer.getTokenType mustBe null
    }

    "tokenize a domain definition" in {
      val lexer = new RiddlLexerAdapter()
      val text = "domain MyDomain is { }"
      lexer.start(text, 0, text.length, 0)

      val tokens = collectTokens(lexer)

      // Should have: keyword(domain), identifier(MyDomain), readability(is),
      // punctuation({), punctuation(})
      tokens.map(_._1) must contain(RiddlTokenTypes.KEYWORD)
      tokens.map(_._1) must contain(RiddlTokenTypes.IDENTIFIER)
      tokens.map(_._1) must contain(RiddlTokenTypes.PUNCTUATION)
    }

    "tokenize comments" in {
      val lexer = new RiddlLexerAdapter()
      val text = "// This is a comment"
      lexer.start(text, 0, text.length, 0)

      lexer.getTokenType mustBe RiddlTokenTypes.COMMENT
    }

    "tokenize quoted strings" in {
      val lexer = new RiddlLexerAdapter()
      val text = "\"hello world\""
      lexer.start(text, 0, text.length, 0)

      lexer.getTokenType mustBe RiddlTokenTypes.QUOTED_STRING
    }

    "tokenize predefined types" in {
      val lexer = new RiddlLexerAdapter()
      val text = "String Integer Boolean UUID"
      lexer.start(text, 0, text.length, 0)

      val tokens = collectTokens(lexer)

      // All should be predefined types
      tokens.foreach { case (tokenType, _) =>
        tokenType mustBe RiddlTokenTypes.PREDEFINED
      }
    }

    "tokenize numeric literals" in {
      val lexer = new RiddlLexerAdapter()
      val text = "42"
      lexer.start(text, 0, text.length, 0)

      lexer.getTokenType mustBe RiddlTokenTypes.NUMERIC
    }

    "tokenize a complete RIDDL snippet" in {
      val lexer = new RiddlLexerAdapter()
      val text =
        """domain Example is {
          |  context UserContext is {
          |    type UserId is Id(User)
          |  }
          |}""".stripMargin
      lexer.start(text, 0, text.length, 0)

      val tokens = collectTokens(lexer)

      // Should tokenize successfully with multiple token types
      tokens.nonEmpty mustBe true
      tokens.map(_._1).toSet.size must be > 1
    }

    "handle syntax errors gracefully" in {
      val lexer = new RiddlLexerAdapter()
      val text = "domain { incomplete"
      lexer.start(text, 0, text.length, 0)

      // Should still return tokens even with syntax errors
      val tokens = collectTokens(lexer)
      tokens.nonEmpty mustBe true
    }

    "track buffer state correctly" in {
      val lexer = new RiddlLexerAdapter()
      val text = "domain Test is { }"
      lexer.start(text, 0, text.length, 0)

      lexer.getBufferSequence mustBe text
      lexer.getBufferEnd mustBe text.length
      lexer.getState mustBe 0
    }
  }

  "RiddlTokenTypes" must {

    "have a FILE element type" in {
      RiddlTokenTypes.FILE must not be null
    }

    "have token sets for comments and strings" in {
      RiddlTokenTypes.COMMENTS.contains(RiddlTokenTypes.COMMENT) mustBe true
      RiddlTokenTypes.STRINGS.contains(RiddlTokenTypes.QUOTED_STRING) mustBe true
      RiddlTokenTypes.STRINGS.contains(RiddlTokenTypes.MARKDOWN_LINE) mustBe true
    }
  }

  /** Helper method to collect all tokens from a lexer. */
  private def collectTokens(
      lexer: RiddlLexerAdapter
  ): List[(com.intellij.psi.tree.IElementType, String)] =
    val tokens = scala.collection.mutable.ListBuffer
      .empty[(com.intellij.psi.tree.IElementType, String)]

    while lexer.getTokenType != null do
      val tokenText = lexer.getBufferSequence
        .subSequence(lexer.getTokenStart, lexer.getTokenEnd)
        .toString
      tokens += ((lexer.getTokenType, tokenText))
      lexer.advance()
    end while

    tokens.toList
}
