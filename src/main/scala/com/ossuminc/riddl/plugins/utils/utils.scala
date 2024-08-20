package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.{Content, ContentManager}
import com.ossuminc.riddl.commands.Commands
import com.ossuminc.riddl.language.{CommonOptions, Messages}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  RiddlIdeaSettingsConfigurable
}
import com.ossuminc.riddl.utils.StringLogger

import java.awt.GridBagConstraints
import scala.jdk.CollectionConverters.*

//case class RiddlIdeaPluginLogger(override val withHighlighting: Boolean = true)
//    extends Logger {
//  import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState
//
//  override def write(level: Logging.Lvl, s: String): Unit = {
//    getRiddlIdeaState.getState.appendOutput(s)
//  }
//}

package object utils {
  object parsing {
    def parseASTFromConfFile(numWindow: Int, confFile: String): Unit = {
      val result: Either[List[Messages.Message], PassesResult] =
        Commands.runCommandWithArgs(
          "from",
          Array(
            "from",
            confFile,
            "validate"
          ),
          StringLogger(),
          CommonOptions(noANSIMessages = true, groupMessagesByKind = true)
        )

      val windowState = getRiddlIdeaState(numWindow)
      windowState.clearOutput()

      result match {
        case Right(result) =>
          windowState.appendOutput(
            s"Success!! There were no errors on project compilation<br>${result.messages.distinct.format
                .replace("\n", "<br>")}"
          )
        case Left(messages) =>
          windowState.appendOutput(
            messages.distinct.format
              .replace("\n", "<br>")
              .replace(" ", "&nbsp;")
          )
      }

      updateToolWindow(numWindow)
    }
  }

  def displayNotification(text: String): Unit = Notifications.Bus.notify(
    new Notification(
      "Riddl Plugin Notification",
      text,
      NotificationType.INFORMATION
    )
  )

  val application: Application = ApplicationManager.getApplication

  def getContentManager: ContentManager = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")
    .getContentManager

  def getToolWindowContent(numWindow: Int): Content =
    getContentManager.getContent(numWindow - 1)

  def updateToolWindow(numWindow: Int, fromReload: Boolean = false): Unit = {
    getToolWindowContent(numWindow).getComponent
      .getClientProperty(s"updateLabel_$numWindow")
      .asInstanceOf[(fromReload: Boolean) => Unit](fromReload)
  }

  def createNewToolWindow(): Unit =
    getToolWindowContent(1).getComponent
      .getClientProperty("createToolWindow")
      .asInstanceOf[() => Unit]()

  def openToolWindowSettings(): Unit = ShowSettingsUtil.getInstance
    .editConfigurable(getProject, new RiddlIdeaSettingsConfigurable)

  def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

  def getRiddlIdeaStates: RiddlIdeaSettings.States =
    application
      .getService(
        classOf[RiddlIdeaSettings]
      )
      .getState

  def getRiddlIdeaState(numToolWindow: Int): RiddlIdeaSettings.State =
    getRiddlIdeaStates.getState(
      numToolWindow
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
