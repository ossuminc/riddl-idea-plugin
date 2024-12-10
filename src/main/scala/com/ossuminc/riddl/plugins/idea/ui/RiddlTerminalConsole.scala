package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState
import com.ossuminc.riddl.plugins.idea.utils.editorForErroneousFile

class RiddlTerminalConsole(
    numWindow: Int,
    project: Project
) extends ConsoleViewImpl(project, true) {
  def printMessages(): Unit = {
    getRiddlIdeaState(numWindow).getMessagesForConsole.values.toSeq.flatten
      .foreach { msg =>
        linkToEditor(
          msg.loc.source.origin,
          numWindow,
          msg.loc.line,
          msg.loc.offset
        )
        super.print(
          msg.format.split(msg.loc.source.origin).last + "\n\n",
          ConsoleViewContentType.NORMAL_OUTPUT
        )
      }
    ()
  }

  private def linkToEditor(
      fileName: String,
      numWindow: Int,
      lineNumber: Int,
      charPosition: Int
  ): Unit = {
    val editor = editorForErroneousFile(numWindow, fileName)

    val hyperlinkInfo = new HyperlinkInfoBase {
      override def navigate(
          project: Project,
          relativePoint: RelativePoint
      ): Unit = if editor.isDefined then {
        val logicalPosition =
          new LogicalPosition(
            lineNumber,
            charPosition
          )
        editor.foreach(
          _.getCaretModel.moveToLogicalPosition(logicalPosition)
        )
      }
    }

    this.printHyperlink(fileName + "\n", hyperlinkInfo)
  }
}
