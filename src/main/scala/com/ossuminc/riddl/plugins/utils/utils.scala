package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.{AST, Messages}
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings

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
    basePath + "/" + confFileName

  private val application = ApplicationManager.getApplication

  def getToolWindow: Content = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")
    .getContentManager
    .getContent(0)

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getRiddlIdeaState: RiddlIdeaSettings =
    application.getService(
      classOf[RiddlIdeaSettings]
    )
}
