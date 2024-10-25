package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileEditorManager,
  FileEditorManagerEvent,
  FileEditorManagerListener,
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getProject
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.highlightKeywordsForFile

class RiddlFileListenerHighlighter extends FileEditorManagerListener {
  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = highlightKeywordsForFile(source, file)

  override def selectionChanged(event: FileEditorManagerEvent): Unit =
    if event.getNewFile != null then
      highlightKeywordsForFile(
        FileEditorManager
          .getInstance(getProject),
        event.getNewFile
      )
}
