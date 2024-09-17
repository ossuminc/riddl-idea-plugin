package com.ossuminc.riddl.plugins.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.{CommonOptions, Messages}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.utils.StringLogger

object ParsingUtils {
  import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*
  import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.*

  def parseASTFromConfFile(numWindow: Int, confFile: String): Unit = {
    val result: Either[List[Messages.Message], PassesResult] =
      Commands.runCommandWithArgs(
        Array(
          "from",
          confFile,
          "validate"
        ),
        StringLogger(),
        getRiddlIdeaState(numWindow).getCommonOptions
      )

    val windowState = getRiddlIdeaState(numWindow)
    windowState.clearOutput()

    result match {
      case Right(result) =>
        windowState.appendOutput(
          s"Success!! There were no errors on project compilation<br>${result.messages.distinct.format
              .replace("\n", "<br>")}"
        )
        updateToolWindow(numWindow)
      case Left(messages) =>
        windowState.appendOutput(
          messages.distinct.format
            .replace("\n", "<br>")
            .replace(" ", "&nbsp;")
        )
        updateToolWindow(numWindow)
    }

  }
}
