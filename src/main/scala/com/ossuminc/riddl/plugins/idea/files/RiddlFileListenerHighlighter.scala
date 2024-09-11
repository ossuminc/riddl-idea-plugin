package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomHighlighterColors
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditor, FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener, FileEditorProvider, FileEditorWithTextEditors, TextEditor}
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.{keywords, punctuation, readability}
import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Document, Editor}
import kotlin.Result
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.getProject
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.util.containers.ContainerUtil
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.colors.{ColorKey, TextAttributesKey}

class RiddlFileListenerHighlighter extends FileEditorManagerListener {
  override def fileOpened(
    source: FileEditorManager,
    file: VirtualFile,
  ): Unit = source.getAllEditors(file).foreach { te =>
      val doc = FileDocumentManager.getInstance().getDocument(file)
      if doc != null then {
        te match {
          case textEditor: TextEditor => highlightKeywords(doc.getText, textEditor.getEditor)
        }
      }
    }

  //override def selectionChanged(
  //   event: FileEditorManagerEvent
  // ): Unit = source.getAllEditors(file).foreach { te =>
  //  val doc = FileDocumentManager.getInstance().getDocument(file)
  //  if doc != null then {
  //    te match {
  //      case textEditor: TextEditor => highlightKeywords(doc.getText, textEditor.getEditor)
  //    }
  //  }
  //}

  private def highlightKeywords(text: String, editor: Editor): Unit = {
    import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*

    def applyColourKey(colorKey: TextAttributesKey, index: Int, length: Int, punctIndex: Int, punct: String): Unit = {
      val (trueIndex: Int, trueLength: Int) =
        if punctIndex < 0 then (index, length) else (punctIndex + index, punct.length)

      editor.getMarkupModel.addRangeHighlighter(
        colorKey,
        trueIndex,
        trueIndex + trueLength,
        HighlighterLayer.SYNTAX,
        HighlighterTargetArea.EXACT_RANGE
      )
      ()
    }

    RiddlTokenizer.tokenize(text).foreach {
      case (token, index, Seq(isQuoted, isComment)) if isQuoted || isComment =>
        applyColourKey(
          if isComment then DefaultLanguageHighlighterColors.LINE_COMMENT
          else if isQuoted then DefaultLanguageHighlighterColors.STRING
          else DefaultLanguageHighlighterColors.KEYWORD,
          index,
          token.length,
          -1,
          ""
        )
      case (token, index, _) if index > -1 && token.length > 0 =>
        if keywords.contains(token) then applyColourKey(CUSTOM_KEYWORD_KEYWORD, index, token.length, -1, "")
        else if punctuation.exists(token.contains) then
          punctuation.find(token.contains).foreach(punct =>
            applyColourKey(CUSTOM_KEYWORD_PUNCTUATION, index, token.length, token.indexOf(punct), punct)
          )
        else if readability.contains(token) then applyColourKey(CUSTOM_KEYWORD_READABILITY, index, token.length, -1, "")
      case (_, _, _) => ()
    }
  }
}

