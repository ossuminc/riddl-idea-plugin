package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.ui.content.{Content, ContentManager}
import com.ossuminc.riddl.command.CommandPlugin
import com.ossuminc.riddl.language.{CommonOptions, Messages}
import com.ossuminc.riddl.passes.PassesResult
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  RiddlIdeaSettingsConfigurable
}
import com.ossuminc.riddl.utils.{Logger, Logging, StringLogger}

import java.awt.GridBagConstraints
import java.io.File
import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*

case class RiddlIdeaPluginLogger(override val withHighlighting: Boolean = true)
    extends Logger {
  import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState

  override def write(level: Logging.Lvl, s: String): Unit = {
    getRiddlIdeaState.getState.appendOutput(s)
  }
}

package object utils {
  object parsing {
    def parseASTFromConfFile(confFile: String): Unit = {
      val result: Either[List[Messages.Message], PassesResult] =
        Commands.runCommandWithArgs(
          "from",
          Array(
            "from",
            navigateFromCanonicalPath(
              confFile
            ),
            "validate"
          ),
          StringLogger(),
          CommonOptions(noANSIMessages = true, groupMessagesByKind = true)
        )

      getRiddlIdeaState.getState.clearOutput()

      result match {
        case Right(result) =>
          getRiddlIdeaState.getState.appendOutput(
            s"Success!! There were no errors on project compilation<br>${result.messages.distinct.format
                .replace("\n", "<br>")}"
          )
        case Left(messages) =>
          println(s"""
               |asdasda
               |${messages.format}
               |asdasda
               |""".stripMargin)
          getRiddlIdeaState.getState.appendOutput(
            messages.distinct.format
              .replace("\n", "<br>")
              .replace(" ", "&nbsp;")
          )
      }

      updateToolWindow()
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

  private def relativizeAbsolutePath(
      filePath: String,
      projectPath: String
  ): String =
    new File(projectPath).toURI
      .relativize(new File(filePath).toURI)
      .getPath

  private def navigateFromCanonicalPath(
      filePathStr: String
  ) = {
    val systemPath: Path =
      Paths.get(System.getProperty("user.dir"))
    val filePath: Path = Paths.get(filePathStr)

    val maxLength =
      math.min(systemPath.getNameCount, filePath.getNameCount)
    val divergence: Int = (0 until maxLength)
      .find(i => systemPath.getName(i) != filePath.getName(i))
      .getOrElse(maxLength)

    systemPath.relativize(filePath).toString
  }
}
