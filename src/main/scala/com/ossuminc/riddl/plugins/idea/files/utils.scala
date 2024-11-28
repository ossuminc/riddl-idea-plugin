package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileDocumentManager,
  FileEditorManager,
  TextEditor
}
import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Editor}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
  HighlighterTargetArea
}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.ossuminc.riddl.language.AST.{
  CommentTKN,
  KeywordTKN,
  OtherTKN,
  PunctuationTKN,
  QuotedStringTKN,
  ReadabilityTKN,
  Token
}
import com.ossuminc.riddl.language.{At, Messages}
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
import com.ossuminc.riddl.plugins.idea.utils.{
  displayNotification,
  highlightForErrorMessage
}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaStates
import com.ossuminc.riddl.utils.StringLogger

object utils {
  private def annotateTokensWithBooleans(
      ast: Either[Messages.Messages, List[Token]]
  ): Seq[(Token, Int, Int, Seq[Boolean])] = ast match {
    case Left(msgs) =>
      displayNotification(msgs.mkString("\n\n"))
      Seq((OtherTKN(At()), 0, 0, Seq(false, false)))
    case Right(tokens) =>
      tokens
        .map(tok => (tok, tok.at.offset, tok.at.endOffset))
        .zip(tokens.map {
          case _: CommentTKN      => Seq(false, true)
          case _: QuotedStringTKN => Seq(true, false)
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
      case _: KeywordTKN =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_KEYWORD,
          offset,
          endOffset - offset
        )
      case _: PunctuationTKN =>
        applyColourKey(editor)(
          CUSTOM_KEYWORD_PUNCTUATION,
          offset,
          endOffset - offset
        )
      case _: ReadabilityTKN =>
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

  def highlightKeywordsAndErrorsForFile(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = source
    .getAllEditors(file)
    .foreach { te =>
      val doc = FileDocumentManager.getInstance().getDocument(file)
      if doc != null then {
        te match {
          case textEditor: TextEditor =>
            highlightKeywords(doc.getText, textEditor.getEditor)
            getRiddlIdeaStates.allStates
              .foldRight(Seq[RiddlIdeaSettings.State]()) { (tup, acc) =>
                tup._2.clearErrorHighlighters()
                if tup._2.getMessagesForEditor.exists(
                    _.loc.source.root.path == file.getPath
                  )
                then acc :+ tup._2
                else acc
              }
              .foreach(state =>
                state.getMessagesForEditor
                  .foreach(msg => highlightForErrorMessage(state, msg))
              )
        }
      }
    }
}
