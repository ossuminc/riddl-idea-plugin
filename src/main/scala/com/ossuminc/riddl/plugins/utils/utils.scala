package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.ui.content.Content
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.{AST, Messages}
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaTopics.MessageListener

import java.net.URI

package object utils {
  def parseASTFromSource(projectURI: URI): Either[Messages, AST.Root] = {
    TopLevelParser
      .parseInput(
        RiddlParserInput(projectURI)
      )
  }

  def displayNotification(text: String): Unit = Notifications.Bus.notify(
    new Notification(
      "Riddl Plugin Notification",
      text,
      NotificationType.INFORMATION
    )
  )

  def fullPathToConf(basePath: String, confFileName: String): String =
    "file://" + basePath + "/src/main/riddl/" + confFileName

  private val application = ApplicationManager.getApplication

  def getToolWindow: ToolWindow = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("RIDDL_TOOL_WINDOW")

  def getToolWindowContent: Content =
    getToolWindow.getContentManagerIfCreated
      .findContent("RIDDL_TOOL_WINDOW")

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getIdFromTopicClass[L <: MessageListener[L]](messageListener: L): String =
    messageListener.listenerTopic.id

  def getRiddlIdeaState: RiddlIdeaSettings.State =
    application.getService(
      classOf[RiddlIdeaSettings.State]
    )
}
