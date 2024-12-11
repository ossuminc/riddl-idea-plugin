package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.ui.awt.RelativePoint
import com.ossuminc.riddl.language.Messages.Message
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState
import com.ossuminc.riddl.plugins.idea.utils.selectedEditor

import java.nio.file.Path

class RiddlTerminalConsole(
    numWindow: Int,
    project: Project
) extends ConsoleViewImpl(project, true) {
  def printMessages(): Unit = {
    val state = getRiddlIdeaState(numWindow)
    state.getMessagesForConsole.values.toSeq.flatten
      .foreach { msg =>
        state.getConfPath.foreach(path =>
          linkToEditor(
            Path.of(path).getParent.toFile.getPath,
            msg
          )
        )
        super.print(
          msg.format.replace(msg.loc.format, "") + "\n\n",
          ConsoleViewContentType.NORMAL_OUTPUT
        )
      }
    ()
  }

  private def linkToEditor(
      confFolder: String,
      msg: Message
  ): Unit = {
    val filePath = confFolder + "/" + msg.loc.source.origin
    val file: VirtualFile = LocalFileSystem.getInstance.findFileByPath(
      filePath
    )

    val hyperlinkInfo = new HyperlinkInfoBase {
      override def navigate(
          project: Project,
          relativePoint: RelativePoint
      ): Unit = {
        val editor: Editor =
          if filePath == selectedEditor.getVirtualFile.getPath
          then selectedEditor
          else
            FileEditorManager
              .getInstance(project)
              .openTextEditor(
                new OpenFileDescriptor(
                  project,
                  file,
                  msg.loc.line - 1,
                  msg.loc.offset
                ),
                true
              )
        val logicalPosition =
          new LogicalPosition(
            msg.loc.line - 1,
            msg.loc.offset
          )
        editor.getCaretModel.moveToLogicalPosition(logicalPosition)
      }
    }

    this.printHyperlink(msg.loc.format, hyperlinkInfo)
  }
}
