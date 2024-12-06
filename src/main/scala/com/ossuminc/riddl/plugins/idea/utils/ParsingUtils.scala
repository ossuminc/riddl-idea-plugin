package com.ossuminc.riddl.plugins.idea.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.utils.{
  JVMPlatformContext,
  Logger,
  Logging,
  PlatformContext
}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import com.ossuminc.riddl.utils.{Await, StringLogger, pc}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

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
  import ManagerBasedGetterUtils.*

  def runCommandForConsole(
      numWindow: Int
  ): Unit = {
    given io: PlatformContext = RiddlPluginPlatformContext(numWindow)

    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)
    windowState.clearRunOutput()
    windowState.setMessagesForConsole(mutable.Seq())
    windowState.clearRunOutput()

    if windowState.getCommand.nonEmpty && (
        (windowState.getCommand != "from" && 
          Seq("about", "info").contains(
            windowState.getCommand
          )
        ) || (
          windowState.getCommand == "from" &&
            windowState.getConfPath.isDefined && 
            windowState.getFromOption.isDefined
        )
      )
    then
      io.withOptions(windowState.getCommonOptions) { _ =>
        Commands.runCommandWithArgs(
          Array(
            windowState.getCommand,
            windowState.getConfPath.getOrElse(""),
            if windowState.getCommand == "from" then
              windowState.getFromOption.get
            else ""
          ).filter(_.nonEmpty)
        ) match {
          case Right(result) =>
            windowState.setMessagesForConsole(mutable.Seq.from(result.messages))
          case Left(msgs) =>
            windowState.setMessagesForConsole(mutable.Seq.from(msgs))
        }
      }
    Thread.sleep(100)
    updateToolWindowRunPane(numWindow, fromReload = true)
  }

  def runCommandForEditor(
      numWindow: Int,
      editorTextOpt: Option[String] = None
  ): Unit = {
    val state = getRiddlIdeaState(numWindow)

    val rpi: RiddlParserInput = editorTextOpt match {
      case Some(editorText) => RiddlParserInput(editorText, "")
      case None if state.getTopLevelPath.isDefined =>
        Await.result(
          RiddlParserInput.fromPath(state.getTopLevelPath.get),
          FiniteDuration(5, TimeUnit.SECONDS)
        )
      case None => return
    }

    pc.withLogger(StringLogger()) { _ =>
      pc.withOptions(state.getCommonOptions) { _ =>
        TopLevelParser.parseNebula(rpi) match {
          case Right(_) =>
            ()
          case Left(msgs) =>
            state.setMessagesForEditor(mutable.Seq.from(msgs))
        }
      }
    }
    Thread.sleep(100)
  }
}
