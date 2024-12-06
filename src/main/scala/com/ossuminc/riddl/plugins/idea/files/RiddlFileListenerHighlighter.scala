package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileDocumentManager,
  FileEditorManager,
  FileEditorManagerEvent,
  FileEditorManagerListener,
  TextEditor
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaStates
}
import com.ossuminc.riddl.plugins.idea.utils.highlightErrorMessagesForFile

class RiddlFileListenerHighlighter extends FileEditorManagerListener {
  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = highlightKeywordsAndErrors(source, file)

  override def selectionChanged(event: FileEditorManagerEvent): Unit =
    if event.getNewFile != null then
      highlightKeywordsAndErrors(
        FileEditorManager
          .getInstance(getProject),
        event.getNewFile
      )

  private def highlightKeywordsAndErrors(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = source
    .getAllEditors(file)
    .foreach { te =>
      val doc = FileDocumentManager.getInstance().getDocument(file)
      if doc != null then {
        te match {
          case textEditor: TextEditor =>
            utils.highlightKeywords(doc.getText, textEditor.getEditor)
            getRiddlIdeaStates.allStates
              .foreach((_, state) =>
                if state.getConfPath.isDefined || state.getTopLevelPath.isDefined
                then
                  highlightErrorMessagesForFile(
                    state,
                    Right(file.getName),
                    state.getConfPath.isDefined
                  )
              )
        }
      }
    }
}
