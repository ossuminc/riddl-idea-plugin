/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.lexer

import com.intellij.psi.tree.{IElementType, IFileElementType, TokenSet}
import com.ossuminc.riddl.plugins.idea.RiddlLanguage
import com.ossuminc.riddl.language.AST.Token

/** Token types for the RIDDL language.
  *
  * These IElementType instances map RIDDL parser tokens to IntelliJ token types
  * for use in syntax highlighting and lexical analysis.
  */
object RiddlTokenTypes {

  /** File element type for RIDDL files. */
  val FILE: IFileElementType = new IFileElementType(RiddlLanguage)

  // Token types corresponding to RIDDL Token classes
  val KEYWORD: IElementType = new IElementType("RIDDL_KEYWORD", RiddlLanguage)
  val IDENTIFIER: IElementType = new IElementType("RIDDL_IDENTIFIER", RiddlLanguage)
  val READABILITY: IElementType = new IElementType("RIDDL_READABILITY", RiddlLanguage)
  val PUNCTUATION: IElementType = new IElementType("RIDDL_PUNCTUATION", RiddlLanguage)
  val PREDEFINED: IElementType = new IElementType("RIDDL_PREDEFINED", RiddlLanguage)
  val COMMENT: IElementType = new IElementType("RIDDL_COMMENT", RiddlLanguage)
  val QUOTED_STRING: IElementType = new IElementType("RIDDL_QUOTED_STRING", RiddlLanguage)
  val MARKDOWN_LINE: IElementType = new IElementType("RIDDL_MARKDOWN_LINE", RiddlLanguage)
  val LITERAL_CODE: IElementType = new IElementType("RIDDL_LITERAL_CODE", RiddlLanguage)
  val NUMERIC: IElementType = new IElementType("RIDDL_NUMERIC", RiddlLanguage)
  val OTHER: IElementType = new IElementType("RIDDL_OTHER", RiddlLanguage)

  /** Maps a RIDDL Token to its corresponding IElementType. */
  def fromRiddlToken(token: Token): IElementType = token match {
    case _: Token.Keyword      => KEYWORD
    case _: Token.Identifier   => IDENTIFIER
    case _: Token.Readability  => READABILITY
    case _: Token.Punctuation  => PUNCTUATION
    case _: Token.Predefined   => PREDEFINED
    case _: Token.Comment      => COMMENT
    case _: Token.QuotedString => QUOTED_STRING
    case _: Token.MarkdownLine => MARKDOWN_LINE
    case _: Token.LiteralCode  => LITERAL_CODE
    case _: Token.Numeric      => NUMERIC
    case _: Token.Other        => OTHER
  }

  // Token sets for various purposes
  val COMMENTS: TokenSet = TokenSet.create(COMMENT)
  val STRINGS: TokenSet = TokenSet.create(QUOTED_STRING, MARKDOWN_LINE, LITERAL_CODE)
  val WHITESPACE: TokenSet = TokenSet.EMPTY // RIDDL lexer handles whitespace internally
}
