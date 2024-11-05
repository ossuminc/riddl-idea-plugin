package com.ossuminc.riddl.plugins.idea.utils

import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils
import com.ossuminc.riddl.utils.{
  Await,
  Logger,
  Logging,
  PathUtils,
  PlatformContext,
  URL,
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
      confFile: String
  ): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    pc.withLogger(RiddlIdeaPluginLogger(numWindow)) { _ =>
      pc.withOptions(getRiddlIdeaState(numWindow).getCommonOptions) { _ =>
        Commands.runCommandWithArgs(
          Array(
            windowState.getCommand,
            confFile,
            if confFile.isEmpty then "" else "validate"
          ).filter(_.nonEmpty)
        ) match {
          case Right(_) if windowState.getCommand == "from" =>
            windowState.prependRunOutput(
              "Success!! There were no errors found\n"
            )
          case Left(msgs) =>
            windowState.prependRunOutput("The following errors were found:\n")
            windowState.setMessages(msgs)
          case _ => ()
        }

        updateToolWindowPanes(numWindow, fromReload = true)
      }
    }

  }

  def runCommandForEditor(
      numWindow: Int
  ): Unit = {
    val windowState: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)
    val url: URL = PathUtils
      .urlFromFullPath(
        Paths
          .get(windowState.getTopLevelPath)
      )

    val rpi: RiddlParserInput = Await.result(
      RiddlParserInput.fromURL(url),
      FiniteDuration(5, TimeUnit.SECONDS)
    )
    val tlp: TopLevelParser = TopLevelParser(rpi, false)

    pc.withLogger(RiddlIdeaPluginLogger(numWindow)) { _ =>
      pc.withOptions(getRiddlIdeaState(numWindow).getCommonOptions) { _ =>
        tlp.parseRootWithURLs match {
          case Right((_, paths)) =>
            println("Right")
            println(paths)
            windowState.setParsedPaths(
              paths.map(url => Paths.get(url.path))
            )
          case Left((msgs, paths)) =>
            println("Left")
            println(msgs)
            windowState.setMessages(msgs)
            windowState.setParsedPaths(
              paths.map(url => Paths.get(url.path))
            )
        }

        updateToolWindowPanes(numWindow, fromReload = true)
      }
    }

  }
}
