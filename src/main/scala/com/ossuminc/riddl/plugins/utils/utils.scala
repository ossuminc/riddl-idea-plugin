package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.ossuminc.riddl.command.CommandPlugin
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.utils.Logger

import scala.jdk.CollectionConverters.*

case class RiddlIdeaPluginLogger(override val withHighlighting: Boolean = true)
    extends Logger {
  import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState

  override def write(level: Logger.Lvl, s: String): Unit = {
    println("updating")
    getRiddlIdeaState.getState.appendOutput(highlight(level, s))
  }
}

package object utils {
  def parseASTFromConfFile(confFile: String): Unit = {
    CommandPlugin.runMain(
      Array("from", confFile, "hugo"),
      RiddlIdeaPluginLogger()
    )
    // updateToolWindow()
  }

  def displayNotification(text: String): Unit = Notifications.Bus.notify(
    new Notification(
      "Riddl Plugin Notification",
      text,
      NotificationType.INFORMATION
    )
  )

  val application = ApplicationManager.getApplication

  def getToolWindow: Content = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")
    .getContentManager
    .getContent(0)

  def updateToolWindow(): Unit = getToolWindow.getComponent
    .getClientProperty("updateLabel")
    .asInstanceOf[() => Unit]()

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getRiddlIdeaState: RiddlIdeaSettings =
    application.getService(
      classOf[RiddlIdeaSettings]
    )
}
