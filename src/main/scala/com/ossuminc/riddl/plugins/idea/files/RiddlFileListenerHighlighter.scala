/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileDocumentManager,
  FileEditorManager,
  FileEditorManagerEvent,
  FileEditorManagerListener,
  TextEditor
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.runCommandForEditor
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaStates
}
import com.ossuminc.riddl.plugins.idea.utils.highlightErrorMessagesForFile

/** Listens for file open/selection events to trigger error highlighting.
  *
  * Note: Syntax highlighting is handled automatically by RiddlSyntaxHighlighter
  * via the IntelliJ lexer infrastructure. This listener only handles error
  * annotation updates when files are opened or selected.
  */
class RiddlFileListenerHighlighter extends FileEditorManagerListener {

  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = highlightErrors(source, file)

  override def selectionChanged(event: FileEditorManagerEvent): Unit =
    if event.getNewFile != null then
      highlightErrors(
        FileEditorManager.getInstance(getProject),
        event.getNewFile
      )

  private def highlightErrors(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = source.getAllEditors(file).foreach { te =>
    val doc = FileDocumentManager.getInstance().getDocument(file)
    if doc != null then
      te match
        case _: TextEditor =>
          getRiddlIdeaStates.allStates.foreach { (_, state) =>
            if state.getTopLevelPath.isDefined then
              runCommandForEditor(state.getWindowNum)
            if state.hasMessages then
              highlightErrorMessagesForFile(
                state,
                Right(file.getPath),
                state.getConfPath.isDefined
              )
          }
        case _ => // Ignore non-text editors
  }
}
