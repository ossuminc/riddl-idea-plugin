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
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
import com.ossuminc.riddl.plugins.idea.utils.isFilePathBelowAnother
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaStates
import com.ossuminc.riddl.utils.StringLogger
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.*

object utils {
  private def annotateTokensWithBooleans(
      ast: Either[Messages.Messages, List[Token]]
  ): Seq[(Token, Int, Int, Seq[Boolean])] = ast match {
    case Left(_) =>
      Seq((Token.Other(At()), 0, 0, Seq(false, false)))
    case Right(tokens) =>
      tokens
        .map(tok => (tok, tok.loc.offset, tok.loc.endOffset))
        .zip(tokens.map {
          case _: Token.Comment      => Seq(false, true)
          case _: Token.QuotedString => Seq(true, false)
          case _                  => Seq(false, false)
        })
        .map((offsetTup, tokSeq) =>
          (offsetTup._1, offsetTup._2, offsetTup._3, tokSeq)
        )
  }

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
  )(token: Token, offset: Int, endOffset: Int, flags: Seq[Boolean]): Unit =
    token match
      case _: Token.Keyword =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_KEYWORD,
          offset,
          endOffset - offset
        )
      case _: Token.Punctuation =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_PUNCTUATION,
          offset,
          endOffset - offset
        )
      case _: Token.Readability =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_READABILITY,
          offset,
          endOffset - offset
        )
      case _ =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.IDENTIFIER,
          offset,
          endOffset - offset
        )

    flags match {
      case Seq(isQuoted, _) if isQuoted =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.STRING,
          offset,
          endOffset - offset
        )
      case Seq(_, isComment) if isComment =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.LINE_COMMENT,
          offset,
          endOffset - offset
        )
      case Seq(_, _) => ()
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
