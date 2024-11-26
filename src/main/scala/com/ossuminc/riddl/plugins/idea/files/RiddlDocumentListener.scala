package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.ossuminc.riddl.plugins.idea.files.utils.highlightKeywords
import com.ossuminc.riddl.plugins.idea.utils.highlightForErrorMessage
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.*

import java.nio.file.Path

class RiddlDocumentListener extends DocumentListener {
  override def documentChanged(event: DocumentEvent): Unit = {
    val doc = event.getDocument

    val editors = EditorFactory.getInstance().getEditors(doc)
    if editors.nonEmpty then
      editors.find(_.getDocument == doc) match
        case Some(editor) if doc.getText.nonEmpty =>
          if editor.getVirtualFile != null then
            val editorFilePath = editor.getVirtualFile.getPath

            highlightKeywords(editor.getDocument.getText, editor)

            getRiddlIdeaStates.allStates.values.toSeq
              .filter { state =>
                state.getTopLevelPath.exists(path =>
                  Path.of(path).compareTo(Path.of(editorFilePath)) <= 0
                )
              }
              .foreach { state =>
                state.getMessages
                  .filter(msg => editorFilePath.endsWith(msg.loc.source.origin))
                  .foreach { msg =>
                    runCommandForEditor(
                      state.getWindowNum,
                      editor.getVirtualFile.getPath
                    )
                    highlightForErrorMessage(
                      state,
                      Right(msg.message),
                      Right((msg.loc, msg.kind.severity))
                    )
                  }
              }

        case _ => ()
  }
}
