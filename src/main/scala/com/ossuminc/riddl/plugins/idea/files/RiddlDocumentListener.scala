package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.ossuminc.riddl.plugins.idea.files.utils.highlightKeywords
import com.intellij.openapi.application.ApplicationManager
import com.ossuminc.riddl.plugins.idea.utils.{
  highlightErrorMessagesForFile,
  isFilePathBelowAnother
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.runCommandForEditor
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RiddlDocumentListener extends DocumentListener {
  override def documentChanged(event: DocumentEvent): Unit = {
    val doc = event.getDocument
    val editors = EditorFactory.getInstance().getEditors(doc)
    if editors.nonEmpty && doc.getText.nonEmpty then
      editors.map { editor =>
        if editor.getVirtualFile != null then {
          getRiddlIdeaStates.allStates.values.toSeq
            .find { state =>
              isFilePathBelowAnother(
                editor.getVirtualFile.getPath,
                state.getTopLevelPath
              )
            }
            .foreach { state =>
              runCommandForEditor(
                state.getWindowNum,
                Some((event.getDocument.getText, editor.getVirtualFile.getPath))
              )
              if state.getMessagesForEditor.nonEmpty
              then highlightErrorMessagesForFile(state, Left(editor))
            }
          highlightKeywords(editor.getDocument.getText, editor)
        }
      }
  }

}
