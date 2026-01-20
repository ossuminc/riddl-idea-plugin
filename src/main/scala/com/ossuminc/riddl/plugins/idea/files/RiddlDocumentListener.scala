/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.ossuminc.riddl.plugins.idea.utils.{
  highlightErrorMessagesForFile,
  isFilePathBelowAnother
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.runCommandForEditor

/** Listens for document changes in RIDDL files to trigger error highlighting.
  *
  * Note: Syntax highlighting is handled automatically by RiddlSyntaxHighlighter
  * via the IntelliJ lexer infrastructure. This listener only handles error
  * annotation updates on document changes.
  */
class RiddlDocumentListener extends DocumentListener {

  override def documentChanged(event: DocumentEvent): Unit =
    val doc = event.getDocument
    val editors = EditorFactory.getInstance().getEditors(doc)
    if editors.nonEmpty && doc.getText.nonEmpty then
      editors.foreach { editor =>
        if editor.getVirtualFile != null then
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
              if state.getMessagesForEditor.nonEmpty then
                highlightErrorMessagesForFile(state, Left(editor))
            }
      }
}
