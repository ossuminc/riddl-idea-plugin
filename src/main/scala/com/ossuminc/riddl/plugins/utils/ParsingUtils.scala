package com.ossuminc.riddl.plugins.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.{CommonOptions, Messages}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.utils.StringLogger

object ParsingUtils {
  import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*
  import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.*

  def runCommandForWindow(numWindow: Int, confFile: Option[String] = None): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    val result: Either[List[Messages.Message], PassesResult] =
      Commands.runCommandWithArgs(
        Array(
          windowState.getCommand,
          if confFile.isDefined then confFile.get else "",
          if confFile.isDefined then "validate" else ""
        ).filter(_.nonEmpty),
        StringLogger(),
        CommonOptions(noANSIMessages = true, groupMessagesByKind = true, showTimes = true)
      )

    windowState.clearOutput()

    result match {
      case Right(result) =>
        windowState.appendOutput(
          s"Success!! There were no errors on project compilation<br>${result.messages.distinct.format
              .replace("\n", "<br>")}"
        )
      case Left(messages) =>
        windowState.appendOutput(
          messages.distinct.format
            .replace("\n", "<br>")
            .replace(" ", "&nbsp;")
        )
    }

    updateToolWindow(numWindow)
  }
}
