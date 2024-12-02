package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.ossuminc.riddl.plugins.idea.utils.riddlErrorRegex
import com.ossuminc.riddl.plugins.idea.utils.editorForError

import scala.util.matching.Regex

class RiddlTerminalConsole(
    numWindow: Int,
    project: Project
) extends ConsoleViewImpl(project, true) {
  override def print(
      text: String,
      contentType: ConsoleViewContentType
  ): Unit =
    text
      .split("\n")
      .toList
      .foreach { line =>
        riddlErrorRegex.findFirstMatchIn(line) match
          case Some(resultMatch: Regex.Match) =>
            linkToEditor(
              line,
              resultMatch.group(2),
              numWindow,
              resultMatch.group(3).toInt,
              resultMatch.group(4).toInt
            )
          case None =>
            super.print(line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
            ()
      }

  private def linkToEditor(
      textLine: String,
      fileName: String,
      numWindow: Int,
      lineNumber: Int,
      charPosition: Int
  ): Unit = {
    val editor = editorForError(
      numWindow,
      fileName,
      lineNumber,
      charPosition
    )

    val hyperlinkInfo = new HyperlinkInfoBase {
      override def navigate(
          project: Project,
          relativePoint: RelativePoint
      ): Unit = if editor != null then {
        val logicalPosition =
          new LogicalPosition(
            lineNumber - 1,
            charPosition - 1
          )
        editor.getCaretModel.moveToLogicalPosition(logicalPosition)
      }
    }

    this.printHyperlink(textLine + "\n", hyperlinkInfo)
  }
}
