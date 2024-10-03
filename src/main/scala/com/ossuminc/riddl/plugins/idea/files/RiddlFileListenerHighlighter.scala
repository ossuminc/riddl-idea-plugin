package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileDocumentManager,
  FileEditorManager,
  FileEditorManagerListener,
  TextEditor
}
import com.intellij.openapi.vfs.VirtualFile

class RiddlFileListenerHighlighter extends FileEditorManagerListener {
  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = source.getAllEditors(file).foreach { te =>
    val doc = FileDocumentManager.getInstance().getDocument(file)
    if doc != null then {
      te match {
        case textEditor: TextEditor =>
          utils.highlightKeywords(doc.getText, textEditor.getEditor)
      }
    }
  }
}
