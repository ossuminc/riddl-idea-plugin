package com.ossuminc.riddl.plugins.idea.utils

import com.intellij.openapi.fileEditor.{
  FileDocumentManager,
  FileEditorManager,
  TextEditor
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.CommonOptions
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.files.utils.highlightKeywords
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils
import com.ossuminc.riddl.utils.{Logger, Logging, StringLogger}

import java.nio.file.{Path, Paths}

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

  def highlightKeywordsForFile(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = source
    .getAllEditors(file)
    .foreach { te =>
      val doc = FileDocumentManager.getInstance().getDocument(file)
      if doc != null then {
        te match {
          case textEditor: TextEditor =>
            textEditor.getEditor.getMarkupModel.removeAllHighlighters()
            highlightKeywords(doc.getText, textEditor.getEditor)
            // TODO: if Messages exist in state relating to doc path, then highlight
            getRiddlIdeaStates.allStates
              .foreach((_, state) => highlightErrorForFile(state, file.getName))
        }
      }
    }

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
      case Left(_) =>
        windowState.prependRunOutput("The following errors were found:\n")
      case _ => ()
    }

    updateToolWindowPanes(numWindow, fromReload = true)
  }
}
