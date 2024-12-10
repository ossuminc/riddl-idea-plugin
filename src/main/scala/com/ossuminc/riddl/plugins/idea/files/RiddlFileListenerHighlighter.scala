package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  FileDocumentManager,
  FileEditorManager,
  FileEditorManagerEvent,
  FileEditorManagerListener,
  TextEditor
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.runCommandForEditor
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaStates
}
import com.ossuminc.riddl.plugins.idea.utils.highlightErrorMessagesForFile

import java.nio.file.Path

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

  private def relativizeLeafToRunPath(
      state: RiddlIdeaSettings.State,
      leafPath: String
  ): Option[String] = (state.getTopLevelPath match {
    case Some(topPath) => Some(Path.of(topPath).getParent)
    case None =>
      state.getConfPath.map(confPath => Path.of(confPath).getParent)
  }).map { folderPath =>
    folderPath.relativize(Path.of(leafPath)).toString
  }

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
                relativizeLeafToRunPath(state, file.getPath).foreach(
                  relativePath =>
                    if state.getTopLevelPath.isDefined then
                      runCommandForEditor(state.getWindowNum)
                    if state.hasMessages
                    then
                      highlightErrorMessagesForFile(
                        state,
                        Right(relativePath),
                        state.getConfPath.isDefined
                      )
                )
              )
        }
      }
    }
}
