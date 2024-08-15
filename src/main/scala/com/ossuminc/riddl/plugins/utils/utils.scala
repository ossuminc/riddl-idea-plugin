package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.ui.content.{Content, ContentManager}
import com.ossuminc.riddl.command.CommandPlugin
import com.ossuminc.riddl.language.{CommonOptions, Messages}
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  RiddlIdeaSettingsConfigurable
}
import com.ossuminc.riddl.utils.{Logger, StringLogger}

import java.awt.GridBagConstraints
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
  def parseASTFromConfFile(numWindow: Int, confFile: String): Unit = {
    CommandPlugin.runMain(
      Array("from", confFile, "hugo"),
      RiddlIdeaPluginLogger()
    )
    updateToolWindow(numWindow)
  }

  def formatParsedResults: String = getRiddlIdeaState.getState.riddlOutput
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
      s"$kind output<br>---------<br>${lines.map(_._2).mkString("<br>")}"
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

  def getToolWindow: ToolWindow = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")

  def getToolWindowManager: ContentManager = getToolWindow.getContentManager

  def getToolWindowContent: Content = getToolWindowManager
    .getContent(0)

  def updateToolWindow(numWindow: Int, fromReload: Boolean = false): Unit = {
    println("finding" + numWindow)
    getToolWindowContent.getComponent
      .getClientProperty(s"updateLabel_$numWindow")
      .asInstanceOf[(fromReload: Boolean) => Unit](fromReload)
  }

  def createNewToolWindow(): Unit =
    getToolWindowContent.getComponent
      .getClientProperty("createToolWindow")
      .asInstanceOf[() => Unit]()

  def openToolWindowSettings(): Unit = ShowSettingsUtil.getInstance
    .editConfigurable(getProject, new RiddlIdeaSettingsConfigurable)

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getRiddlIdeaState: RiddlIdeaSettings =
    application.getService(
      classOf[RiddlIdeaSettings]
    )

  def createGBCs(
      gridX: Int,
      gridY: Int,
      weightX: Int,
      wightY: Int,
      fill: Int
  ): GridBagConstraints = {
    val newGBCs = new GridBagConstraints()
    newGBCs.gridx = gridX
    newGBCs.gridy = gridY
    newGBCs.weightx = weightX
    newGBCs.weighty = wightY
    newGBCs.fill = fill
    newGBCs
  }
}
