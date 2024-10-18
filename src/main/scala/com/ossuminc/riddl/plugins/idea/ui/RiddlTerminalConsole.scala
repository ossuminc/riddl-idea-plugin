package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.execution.filters.{HyperlinkInfo, HyperlinkInfoBase}
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.awt.RelativePoint

import scala.util.matching.Regex

class RiddlTerminalConsole(
    numWindow: Int,
    project: Project
) extends ConsoleViewImpl(project, true) {
  override def print(
      text: String,
      contentType: ConsoleViewContentType
  ): Unit = {
    val linePattern = """\[\w+\] ([\w/_-]+\.riddl)\((\d+):(\d+)\)\:""".r
    text
      .split("\n")
      .toList
      .foreach { line =>
        linePattern.findFirstMatchIn(line) match
          case Some(resultMatch: Regex.Match) =>
            linkToEditor(
              line,
              resultMatch.group(1),
              resultMatch.group(2).toInt,
              resultMatch.group(3).toInt
            )
          case None =>
            super.print(line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
      }
    ()
  }

  private def linkToEditor(
      textLine: String,
      fileName: String,
      lineNumber: Int,
      charNumber: Int
  ): Unit = {
    import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState

    val pathToConf = getRiddlIdeaState(numWindow).getConfPath
      .split("/")
      .dropRight(1)
      .mkString("/")

    val file: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        s"$pathToConf/$fileName"
      )

    val hyperlinkInfo = new HyperlinkInfoBase {
      override def navigate(
          project: Project,
          relativePoint: RelativePoint
      ): Unit = {
        val editor: Editor = FileEditorManager
          .getInstance(project)
          .openTextEditor(
            new OpenFileDescriptor(
              project,
              file,
              lineNumber - 1,
              charNumber - 1
            ),
            true
          )
        if editor != null then {
          val logicalPosition =
            new LogicalPosition(
              lineNumber - 1,
              charNumber - 1
            )
          editor.getCaretModel.moveToLogicalPosition(logicalPosition)
        }
      }
    }
    this.printHyperlink(textLine + "\n", hyperlinkInfo)
  }
}
