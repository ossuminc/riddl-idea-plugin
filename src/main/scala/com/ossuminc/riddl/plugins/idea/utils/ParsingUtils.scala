package com.ossuminc.riddl.plugins.idea.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState
import com.ossuminc.riddl.utils.{
  JVMPlatformContext,
  Logger,
  Logging,
  PlatformContext
}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

case class RiddlIdeaPluginLogger(
    numWindow: Int
)(using io: PlatformContext)
    extends Logger(using io: PlatformContext) {
  import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState

  override def write(level: Logging.Lvl, s: String): Unit = {
    super.count(level)
    getRiddlIdeaState(numWindow).appendRunOutput(highlight(level, s))
  }
}

class RiddlPluginPlatformContext(numWindow: Int) extends JVMPlatformContext {
  override def ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def log: Logger = RiddlIdeaPluginLogger(numWindow)
}

object ParsingUtils {
  import ToolWindowUtils.*

  def runCommandForWindow(
      numWindow: Int,
      confFile: Option[String] = None
  ): Unit = {
    given io: PlatformContext = RiddlPluginPlatformContext(numWindow)

    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)
    windowState.clearRunOutput()
    windowState.setMessages(mutable.Seq())
    windowState.clearRunOutput()

    if windowState.getCommand.nonEmpty && (
        (windowState.getCommand != "from" &&
          Seq("about", "info").contains(windowState.getCommand)) ||
          (windowState.getCommand == "from" &&
            (confFile.isDefined || windowState.getFromOption.isDefined))
      )
    then
      io.withOptions(windowState.getCommonOptions) { _ =>
        Commands.runCommandWithArgs(
          Array(
            windowState.getCommand,
            confFile.getOrElse(""),
            if windowState.getCommand == "from" then
              windowState.getFromOption.getOrElse("")
            else ""
          ).filter(_.nonEmpty)
        ) match {
          case Right(result) =>
            println("Right: " + result.outputs.messages)
            windowState.setMessages(mutable.Seq.from(result.messages))
          case Left(msgs) =>
            println("Left: " + msgs.mkString("\n"))
            windowState.setMessages(mutable.Seq.from(msgs))
        }
      }
    updateToolWindowRunPane(numWindow, fromReload = true)
  }
}
