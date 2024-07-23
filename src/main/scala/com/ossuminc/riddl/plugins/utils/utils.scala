package com.ossuminc.riddl.plugins

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
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
import scala.jdk.CollectionConverters.*

package object utils {
  def parseASTFromSource(projectURI: URI): Either[Messages, AST.Root] = {
    TopLevelParser
      .parseInput(
        RiddlParserInput(projectURI)
      )
  }

  def parseFromCmdLine(projectURI: URI): String = {
    val cmdProcess = new GeneralCommandLine()
    cmdProcess.addParameter("riddlc")
    cmdProcess.addParameter("from")
    cmdProcess.addParameter(s"\n${projectURI.toString}\n")
    val output = ExecUtil.execAndGetOutput(cmdProcess)
    (if output.getExitCode == 0 then
      output.getStdoutLines
    else output.getStderrLines).asScala.mkString("<br>")
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
