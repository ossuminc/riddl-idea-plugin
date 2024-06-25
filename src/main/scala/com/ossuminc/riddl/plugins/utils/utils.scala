package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.ToolWindowManager
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.{AST, Messages}
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  RiddlIdeaSettingsConfigurable
}
import com.ossuminc.riddl.plugins.idea.ui.RiddlToolWindowFactory

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
    basePath + "/src/main/riddl/" + confFileName

  private val application = ApplicationManager.getApplication

  def getRiddlIdeaState: RiddlIdeaSettings.State =
    application.getService(
      classOf[RiddlIdeaSettings.State]
    )

  def getToolWindow: RiddlToolWindowFactory =
    ToolWindowManager
      .getInstance(
        getProject
      )
      .getToolWindow("RIDDL_TOOL_WINDOW")
      .getContentManagerIfCreated
      .getContent(0)
      .asInstanceOf[RiddlToolWindowFactory]

  def getSettingsConfigurable: RiddlIdeaSettingsConfigurable =
    application.getService(
      classOf[RiddlIdeaSettingsConfigurable]
    )

  def getProject: Project =
    ProjectManager.getInstance().getDefaultProject
}
