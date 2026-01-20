/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.ossuminc.riddl.language.AST.Token
import com.ossuminc.riddl.language.At
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.plugins.idea.highlighting.RiddlColors
import com.ossuminc.riddl.utils.NullLogger

/** Legacy manual highlighting utilities.
  *
  * @deprecated Use RiddlSyntaxHighlighter with RiddlLexerAdapter instead.
  *             The IntelliJ lexer-based highlighting is now the primary mechanism.
  *             This code is kept for backward compatibility with RiddlFileListenerHighlighter.
  */
object utils {

  @deprecated("Use lexer-based highlighting via RiddlSyntaxHighlighter", "1.1.0")
  def highlightKeywords(docText: String, editor: Editor): Unit =
    import com.ossuminc.riddl.utils.pc

    pc.withLogger(NullLogger()) { _ =>
      val rpi = RiddlParserInput(docText, "")
      TopLevelParser.parseToTokens(rpi, withVerboseFailures = false) match
        case Left(_)       => Seq(Token.Other(At(0, docText.length, rpi)))
        case Right(tokens) => tokens.foreach(applyColorToToken(editor))
      end match
    }
  end highlightKeywords

  private def applyColorToToken(editor: Editor)(token: Token): Unit =
    val offset = token.loc.offset
    val endOffset = token.loc.endOffset

    token match
      case _: Token.Punctuation =>
        applyColourKey(editor)(RiddlColors.PUNCTUATION, offset, endOffset - offset)
      case _: Token.Identifier =>
        applyColourKey(editor)(RiddlColors.IDENTIFIER, offset, endOffset - offset)
      case _: Token.Keyword =>
        applyColourKey(editor)(RiddlColors.KEYWORD, offset, endOffset - offset)
      case _: Token.Readability =>
        applyColourKey(editor)(RiddlColors.READABILITY, offset, endOffset - offset)
      case _: Token.Predefined =>
        applyColourKey(editor)(RiddlColors.PREDEFINED, offset, endOffset - offset)
      case _: Token.Comment =>
        applyColourKey(editor)(RiddlColors.COMMENT, offset, endOffset - offset)
      case _: Token.MarkdownLine =>
        applyColourKey(editor)(RiddlColors.MARKDOWN, offset, endOffset - offset)
      case _: Token.QuotedString =>
        applyColourKey(editor)(RiddlColors.STRING, offset, endOffset - offset)
      case _: Token.LiteralCode =>
        applyColourKey(editor)(RiddlColors.LITERAL_CODE, offset, endOffset - offset)
      case _: Token.Numeric =>
        applyColourKey(editor)(RiddlColors.NUMERIC, offset, endOffset - offset)
      case _: Token.Other =>
        applyColourKey(editor)(RiddlColors.OTHER, offset, endOffset - offset)

  private def applyColourKey(editor: Editor)(
      colorKey: TextAttributesKey,
      index: Int,
      length: Int
  ): Unit =
    editor.getMarkupModel.getAllHighlighters
      .find(_.getStartOffset == index)
      .foreach(highlighter => editor.getMarkupModel.removeHighlighter(highlighter))

    editor.getMarkupModel.addRangeHighlighter(
      colorKey,
      index,
      index + length,
      HighlighterLayer.FIRST,
      HighlighterTargetArea.EXACT_RANGE
    )
    editor.getContentComponent.repaint()
}
