package com.ossuminc.riddl.plugins.idea.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.AST.Root
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.passes.{Pass, PassesResult}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.utils.{
  Await,
  Logger,
  Logging,
  PlatformContext,
  StringLogger,
  pc
}

import java.nio.file.Paths
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
      numWindow: Int
  ): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)
    windowState.getTopLevelPath.foreach { path =>
      println(path)
      val rpi: RiddlParserInput = Await.result(
        RiddlParserInput.fromPath(path),
        FiniteDuration(5, TimeUnit.SECONDS)
      )

      pc.withLogger(StringLogger()) { _ =>
        pc.withOptions(getRiddlIdeaState(numWindow).getCommonOptions) { _ =>
          TopLevelParser(rpi, false).parseRootWithURLs match {
            case Right((root, _)) =>
              val passesResult = Pass.runStandardPasses(root)
              if passesResult.messages.hasErrors then
                println("passesresult")
                windowState.setMessagesForEditor(
                  passesResult.messages.justErrors
                )
            case Left((msgs, _)) =>
              windowState.setMessagesForEditor(msgs)
              println()
          }
        }
      }
    }
  }
}
