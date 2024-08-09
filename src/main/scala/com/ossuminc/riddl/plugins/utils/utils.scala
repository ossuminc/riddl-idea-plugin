package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.ossuminc.riddl.command.CommandPlugin
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  RiddlIdeaSettingsConfigurable
}
import com.ossuminc.riddl.utils.Logger

import scala.jdk.CollectionConverters.*

case class RiddlIdeaPluginLogger(override val withHighlighting: Boolean = true)
    extends Logger {
  import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState

  override def write(level: Logger.Lvl, s: String): Unit = {
    getRiddlIdeaState.getState.appendOutput(
      fansi
        .Str(highlight(level, s))
        .plainText
    )
  }
}

package object utils {
  def parseASTFromConfFile(confFile: String): Unit = {
    CommandPlugin.runMain(
      Array("from", confFile, "hugo"),
      RiddlIdeaPluginLogger()
    )
    updateToolWindow()
  }

  def formatParsedResults(results: Seq[String]): String = results
    .map { line =>
      val lineArr = line.split("]", 2)
      if lineArr.length > 1 then
        (
          lineArr.head.split("\\[")(1).map(char => char.toUpper),
          lineArr(1)
        )
      else ("", line)
    }
    .groupBy(_._1)
    .map { (kind, lines) =>
      s"$kind output<br>---------<br>${lines.map(_._2.split("\\(0-9)").).mkString("<br>")}"
    }
    .mkString("<br><br>")

  def displayNotification(text: String): Unit = Notifications.Bus.notify(
    new Notification(
      "Riddl Plugin Notification",
      text,
      NotificationType.INFORMATION
    )
  )

  val application: Application = ApplicationManager.getApplication

  def getToolWindow: Content = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")
    .getContentManager
    .getContent(0)

  def updateToolWindow(fromReload: Boolean = false): Unit =
    getToolWindow.getComponent
      .getClientProperty("updateLabel")
      .asInstanceOf[(fromReload: Boolean) => Unit](fromReload)

  def openToolWindowSettings(): Unit = ShowSettingsUtil.getInstance
    .editConfigurable(getProject, new RiddlIdeaSettingsConfigurable)

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getRiddlIdeaState: RiddlIdeaSettings =
    application.getService(
      classOf[RiddlIdeaSettings]
    )
}
