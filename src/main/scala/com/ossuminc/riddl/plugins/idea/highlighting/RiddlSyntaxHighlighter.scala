/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.{SyntaxHighlighter, SyntaxHighlighterBase}
import com.intellij.psi.tree.IElementType
import com.ossuminc.riddl.plugins.idea.lexer.{RiddlLexerAdapter, RiddlTokenTypes}

/** Syntax highlighter for RIDDL files.
  *
  * Maps RIDDL token types to IntelliJ color attributes for syntax highlighting.
  */
class RiddlSyntaxHighlighter extends SyntaxHighlighterBase {

  override def getHighlightingLexer: Lexer = new RiddlLexerAdapter()

  override def getTokenHighlights(tokenType: IElementType): Array[TextAttributesKey] = {
    import RiddlTokenTypes.*

    tokenType match {
      case KEYWORD      => Array(RiddlColors.KEYWORD)
      case IDENTIFIER   => Array(RiddlColors.IDENTIFIER)
      case READABILITY  => Array(RiddlColors.READABILITY)
      case PUNCTUATION  => Array(RiddlColors.PUNCTUATION)
      case PREDEFINED   => Array(RiddlColors.PREDEFINED)
      case COMMENT      => Array(RiddlColors.COMMENT)
      case QUOTED_STRING => Array(RiddlColors.STRING)
      case MARKDOWN_LINE => Array(RiddlColors.MARKDOWN)
      case LITERAL_CODE => Array(RiddlColors.LITERAL_CODE)
      case NUMERIC      => Array(RiddlColors.NUMERIC)
      case OTHER        => Array(RiddlColors.OTHER)
      case _            => Array.empty
    }
  }
}

/** Color attribute keys for RIDDL syntax highlighting.
  *
  * These keys define the actual colors used, with fallback to standard IDE colors.
  * Users can customize these in Settings > Editor > Color Scheme > RIDDL.
  */
object RiddlColors {
  import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

  val KEYWORD: TextAttributesKey =
    createTextAttributesKey("RIDDL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

  val IDENTIFIER: TextAttributesKey =
    createTextAttributesKey("RIDDL_IDENTIFIER", DefaultLanguageHighlighterColors.CLASS_NAME)

  val READABILITY: TextAttributesKey =
    createTextAttributesKey("RIDDL_READABILITY", DefaultLanguageHighlighterColors.METADATA)

  val PUNCTUATION: TextAttributesKey =
    createTextAttributesKey("RIDDL_PUNCTUATION", DefaultLanguageHighlighterColors.OPERATION_SIGN)

  val PREDEFINED: TextAttributesKey =
    createTextAttributesKey("RIDDL_PREDEFINED", DefaultLanguageHighlighterColors.CONSTANT)

  val COMMENT: TextAttributesKey =
    createTextAttributesKey("RIDDL_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)

  val STRING: TextAttributesKey =
    createTextAttributesKey("RIDDL_STRING", DefaultLanguageHighlighterColors.STRING)

  val MARKDOWN: TextAttributesKey =
    createTextAttributesKey("RIDDL_MARKDOWN", DefaultLanguageHighlighterColors.DOC_COMMENT)

  val LITERAL_CODE: TextAttributesKey =
    createTextAttributesKey("RIDDL_LITERAL_CODE", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR)

  val NUMERIC: TextAttributesKey =
    createTextAttributesKey("RIDDL_NUMERIC", DefaultLanguageHighlighterColors.NUMBER)

  val OTHER: TextAttributesKey =
    createTextAttributesKey("RIDDL_OTHER", DefaultLanguageHighlighterColors.IDENTIFIER)
}
