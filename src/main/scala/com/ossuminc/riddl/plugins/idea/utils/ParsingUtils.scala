package com.ossuminc.riddl.plugins.idea.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.utils.{
  Await,
  Logger,
  Logging,
  PlatformContext,
  StringLogger,
  pc
}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

case class RiddlIdeaPluginLogger(
    numWindow: Int
)(using io: PlatformContext)
    extends Logger(using io: PlatformContext) {

  import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState

  override def write(level: Logging.Lvl, s: String): Unit = {
    val state = getRiddlIdeaState(numWindow)
    state.appendRunOutput(highlight(level, s))
  }
}

object ParsingUtils {
  import ManagerBasedGetterUtils.*
  import ToolWindowUtils.*

  // TODO: switch to using runCommandNamed
  def runCommandForWindow(
      numWindow: Int,
      confFile: Option[String]
  ): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    if !windowState.getCommand.isBlank ||
      (windowState.getCommand == "from" && confFile.isDefined && windowState.getFromOption.isDefined)
    then
      pc.withLogger(RiddlIdeaPluginLogger(numWindow)) { _ =>
        pc.withOptions(getRiddlIdeaState(numWindow).getCommonOptions) { _ =>
          Commands.runCommandWithArgs(
            Array(
              windowState.getCommand,
              confFile.getOrElse(""),
              windowState.getFromOption.getOrElse("")
            ).filter(_.nonEmpty)
          ) match {
            case Right(_) if windowState.getCommand == "from" =>
              windowState.prependRunOutput(
                "Success!! There were no errors found\n"
              )
            case Left(_) =>
              windowState.prependRunOutput("The following errors were found:\n")
            case _ => ()
          }

          updateToolWindowRunPane(numWindow, fromReload = true)
        }
      }
  }

  def runCommandForEditor(
      numWindow: Int,
      path: String
  ): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)
    val rpi: RiddlParserInput = Await.result(
      RiddlParserInput.fromPath(path),
      FiniteDuration(5, TimeUnit.SECONDS)
    )

    pc.withLogger(StringLogger()) { _ =>
      pc.withOptions(getRiddlIdeaState(numWindow).getCommonOptions) { _ =>
        TopLevelParser.parseNebula(rpi, false) match {
          case Right(_) => ()
          case Left(msgs) =>
            windowState.setMessages(msgs)
        }
      }
    }

    highlightErrorForFile(windowState, path)
  }
}
