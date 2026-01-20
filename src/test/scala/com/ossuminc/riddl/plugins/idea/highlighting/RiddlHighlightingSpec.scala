/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.highlighting

import com.ossuminc.riddl.plugins.idea.lexer.{RiddlLexerAdapter, RiddlTokenTypes}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for RIDDL syntax highlighting.
  *
  * These tests verify color attribute mappings and highlighter configuration.
  */
class RiddlHighlightingSpec extends AnyWordSpec with Matchers {

  "RiddlSyntaxHighlighter" must {

    "return a lexer" in {
      val highlighter = new RiddlSyntaxHighlighter()
      highlighter.getHighlightingLexer must not be null
      highlighter.getHighlightingLexer.isInstanceOf[RiddlLexerAdapter] mustBe true
    }

    "return highlights for keyword token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.KEYWORD)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.KEYWORD)
    }

    "return highlights for identifier token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.IDENTIFIER)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.IDENTIFIER)
    }

    "return highlights for readability token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.READABILITY)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.READABILITY)
    }

    "return highlights for punctuation token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.PUNCTUATION)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.PUNCTUATION)
    }

    "return highlights for predefined token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.PREDEFINED)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.PREDEFINED)
    }

    "return highlights for comment token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.COMMENT)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.COMMENT)
    }

    "return highlights for quoted string token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.QUOTED_STRING)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.STRING)
    }

    "return highlights for markdown token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.MARKDOWN_LINE)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.MARKDOWN)
    }

    "return highlights for literal code token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.LITERAL_CODE)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.LITERAL_CODE)
    }

    "return highlights for numeric token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.NUMERIC)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.NUMERIC)
    }

    "return highlights for other token" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(RiddlTokenTypes.OTHER)

      highlights.nonEmpty mustBe true
      highlights must contain(RiddlColors.OTHER)
    }

    "return empty array for unknown token type" in {
      val highlighter = new RiddlSyntaxHighlighter()
      val highlights = highlighter.getTokenHighlights(null)

      highlights mustBe empty
    }
  }

  "RiddlColors" must {

    "have all required color keys defined" in {
      RiddlColors.KEYWORD must not be null
      RiddlColors.IDENTIFIER must not be null
      RiddlColors.READABILITY must not be null
      RiddlColors.PUNCTUATION must not be null
      RiddlColors.PREDEFINED must not be null
      RiddlColors.COMMENT must not be null
      RiddlColors.STRING must not be null
      RiddlColors.MARKDOWN must not be null
      RiddlColors.LITERAL_CODE must not be null
      RiddlColors.NUMERIC must not be null
      RiddlColors.OTHER must not be null
    }

    "have unique external names" in {
      val names = Seq(
        RiddlColors.KEYWORD.getExternalName,
        RiddlColors.IDENTIFIER.getExternalName,
        RiddlColors.READABILITY.getExternalName,
        RiddlColors.PUNCTUATION.getExternalName,
        RiddlColors.PREDEFINED.getExternalName,
        RiddlColors.COMMENT.getExternalName,
        RiddlColors.STRING.getExternalName,
        RiddlColors.MARKDOWN.getExternalName,
        RiddlColors.LITERAL_CODE.getExternalName,
        RiddlColors.NUMERIC.getExternalName,
        RiddlColors.OTHER.getExternalName
      )

      names.distinct.size mustBe names.size
    }

    "have RIDDL prefix in external names" in {
      RiddlColors.KEYWORD.getExternalName must startWith("RIDDL_")
      RiddlColors.IDENTIFIER.getExternalName must startWith("RIDDL_")
      RiddlColors.COMMENT.getExternalName must startWith("RIDDL_")
    }
  }

  "RiddlSyntaxHighlighterFactory" must {

    "create a syntax highlighter" in {
      val factory = new RiddlSyntaxHighlighterFactory()
      val highlighter = factory.getSyntaxHighlighter(null, null)

      highlighter must not be null
      highlighter.isInstanceOf[RiddlSyntaxHighlighter] mustBe true
    }
  }
}
