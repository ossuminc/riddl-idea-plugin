package com.ossuminc.riddl.plugins

import com.intellij.execution.TaskExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.{
  OSProcessHandler,
  ProcessAdapter,
  ProcessEvent,
  ProcessOutput,
  ProcessOutputTypes,
  ProcessWaitFor,
}
import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.util.Key
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

  def parseFromCmdLine(confPath: String): String = {
    val cmdProcess = new GeneralCommandLine()

    if getRiddlIdeaState.getState.riddlExePath.isBlank then
      return "Exe path not set - Cannot run"
    else cmdProcess.setExePath(getRiddlIdeaState.getState.riddlExePath)

    cmdProcess.addParameter("from")
    cmdProcess.addParameter(confPath)
    cmdProcess.addParameter("hugo")
    
    val processHandler = OSProcessHandler(cmdProcess)
    val waitFor = new ProcessWaitFor(
      processHandler.getProcess,
      (task: Runnable) =>
        ApplicationManager.getApplication.executeOnPooledThread(task),
      cmdProcess.getCommandLineString()
    )
    val output = new ProcessOutput()

    processHandler.addProcessListener(new ProcessAdapter {
      override def onTextAvailable(
          event: ProcessEvent,
          outputType: Key[?]
      ): Unit = {
        if outputType == ProcessOutputTypes.STDOUT then
          output.appendStdout(event.getText)
        else if outputType == ProcessOutputTypes.STDERR then
          output.appendStderr(event.getText)
      }
    })

    processHandler.startNotify()
    waitFor.waitFor()

    if output.getExitCode == 0 then output.getStdout
    else output.getStderr
  }

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

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getRiddlIdeaState: RiddlIdeaSettings =
    application.getService(
      classOf[RiddlIdeaSettings]
    )
}
