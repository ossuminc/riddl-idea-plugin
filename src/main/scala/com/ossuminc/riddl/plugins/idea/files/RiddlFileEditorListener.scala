package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileEditorManager,
  FileEditorManagerEvent,
  FileEditorManagerListener,
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getProject
import com.ossuminc.riddl.plugins.idea.files.utils.highlightKeywordsAndErrorsForFile

class RiddlFileEditorListener extends FileEditorManagerListener {
  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = highlightKeywordsAndErrorsForFile(source, file)

  override def selectionChanged(event: FileEditorManagerEvent): Unit =
    if event.getNewFile != null then
      highlightKeywordsAndErrorsForFile(
        FileEditorManager
          .getInstance(getProject),
        event.getNewFile
      )
}
