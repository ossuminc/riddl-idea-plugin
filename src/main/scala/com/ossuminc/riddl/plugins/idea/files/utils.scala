package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Editor}
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
  HighlighterTargetArea
}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.ossuminc.riddl.language.AST.Token
import com.ossuminc.riddl.language.{At, Messages}
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.plugins.idea.files.RiddlColorKeywords.*
import com.ossuminc.riddl.plugins.idea.files.utils.annotateTokensWithBooleans
import com.ossuminc.riddl.utils.StringLogger

object utils {
  private def annotateTokensWithBooleans(
      ast: Either[Messages.Messages, List[Token]]
  ): Seq[Token] =
    ast match
      case Left(_)       => Seq(Token.Other(At.empty))
      case Right(tokens) => tokens
    end match
  end annotateTokensWithBooleans

  def highlightKeywords(docText: String, editor: Editor): Unit = {
    import com.ossuminc.riddl.utils.pc

    annotateTokensWithBooleans(
      pc.withLogger(StringLogger()) { _ =>
        TopLevelParser.parseToTokens(
          RiddlParserInput(
            docText,
            ""
          )
        )
      }
    ).foreach(applyColorToToken(editor))
  }

  private def applyColorToToken(
      editor: Editor
  )(token: Token): Unit = {
    val offset = token.loc.offset
    val endOffset = token.loc.endOffset

    token match
      case _: Token.Identifier =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.IDENTIFIER,
          offset,
          endOffset - offset
        )
      case _: Token.Keyword =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_KEYWORD,
          offset,
          endOffset - offset
        )
      case _: Token.Readability =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_READABILITY,
          offset,
          endOffset - offset
        )
      case _: Token.Comment =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.BLOCK_COMMENT,
          offset,
          endOffset - offset
        )
      case _: Token.MarkdownLine =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.STRING,
          offset,
          endOffset - offset
        )
      case _: Token.QuotedString =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.STRING,
          offset,
          endOffset - offset
        )
      case _: Token.LiteralCode =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR,
          offset,
          endOffset - offset
        )
      case _: Token.Punctuation =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_PUNCTUATION,
          offset,
          endOffset - offset
        )
      case _: Token.Numeric =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.LINE_COMMENT,
          offset,
          endOffset - offset
        )
      case _ =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.CLASS_NAME,
          offset,
          endOffset - offset
        )
  }

  private def applyColourKey(editor: Editor)(
      colorKey: TextAttributesKey,
      index: Int,
      length: Int
  ): Unit = {
    editor.getMarkupModel.getAllHighlighters
      .find(_.getStartOffset == index)
      .foreach(highlighter =>
        editor.getMarkupModel.removeHighlighter(highlighter)
      )

    editor.getMarkupModel.addRangeHighlighter(
      colorKey,
      index,
      index + length,
      HighlighterLayer.FIRST,
      HighlighterTargetArea.EXACT_RANGE
    )
    editor.getContentComponent.repaint()
  }
}
