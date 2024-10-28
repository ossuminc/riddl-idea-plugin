package com.ossuminc.riddl.plugins.idea.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils
import com.ossuminc.riddl.utils.{Logger, Logging}

case class RiddlIdeaPluginLogger(
    numWindow: Int,
    override val withHighlighting: Boolean = true
) extends Logger {

  import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState

  override def write(level: Logging.Lvl, s: String): Unit = {
    val state = getRiddlIdeaState(numWindow)
    state.appendRunOutput(highlight(level, s))
  }
}

object ParsingUtils {
  import ManagerBasedGetterUtils.*
  import ToolWindowUtils.*

  def runCommandForWindow(
      numWindow: Int,
      confFile: Option[String] = None
  ): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    Commands.runCommandWithArgs(
      Array(
        windowState.getCommand,
        confFile.getOrElse(""),
        if confFile.isDefined then "validate" else ""
      ).filter(_.nonEmpty),
      RiddlIdeaPluginLogger(numWindow),
      getRiddlIdeaState(numWindow).getCommonOptions
    ) match {
      case Right(_) if windowState.getCommand == "from" =>
        windowState.prependRunOutput("Success!! There were no errors found\n")
      case Left(msgs) =>
        windowState.prependRunOutput("The following errors were found:\n")
        windowState.setMessages(msgs)
      case _ => ()
    }

    updateToolWindowPanes(numWindow, fromReload = true)
  }
}
