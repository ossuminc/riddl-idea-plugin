package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomHighlighterColors
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.fileEditor.{FileDocumentManager, TextEditor, FileEditor, FileEditorManager, FileEditorManagerListener, FileEditorProvider, FileEditorWithTextEditors}
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.{keywords, punctuation, readability}
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import kotlin.Result
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.getProject
import com.intellij.openapi.editor.markup.{HighlighterTargetArea, HighlighterLayer}
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.util.containers.ContainerUtil
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.colors.TextAttributesKey

class RiddlFileListenerHighlighter extends FileEditorManagerListener {
  override def fileOpened(
    source: FileEditorManager,
    file: VirtualFile,
  ): Unit = {
    source.getAllEditors(file).foreach { te =>
      val doc = FileDocumentManager.getInstance().getDocument(file)
      if doc != null then {
        te match {
          case textEditor: TextEditor => highlightKeywords(doc.getText, textEditor.getEditor)
        }
      }
    }
  }

  private def highlightKeywords(text: String, editor: Editor): Unit = {
    import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*

    RiddlTokenizer.tokenize(text).foreach { (token, index, length) =>
      if index > -1 && length > 0 then {
        (if keywords.contains(token) then
          Some(CUSTOM_KEYWORD2)
        else if punctuation.contains(token) then
          Some(CUSTOM_KEYWORD3)
        else if readability.contains(token) then
          Some(CUSTOM_KEYWORD4)
        else None) match {
          case Some(colorKey: TextAttributesKey) =>
            editor.getMarkupModel.addRangeHighlighter(
              colorKey,
              index,
              index + length,
              HighlighterLayer.SYNTAX,
              HighlighterTargetArea.EXACT_RANGE
            )
          case None => ()
        }
      }
    }
  }
}

