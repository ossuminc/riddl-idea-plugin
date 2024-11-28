package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
  MarkupModel,
  TextAttributes
}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.util.ui.UIUtil
import com.ossuminc.riddl.language.Messages
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.updateToolWindowRunPane
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaState
}
import com.typesafe.config.ConfigObject
import pureconfig.ConfigSource

import scala.jdk.CollectionConverters.*
import java.awt.GridBagConstraints
import java.nio.file.Path
import javax.swing.Icon
import scala.util.matching.Regex

package object utils {
  def RiddlIcon[T <: Class[?]](classType: T): Icon =
    IconLoader.getIcon("images/RIDDL-icon.jpg", classType)

  object ManagerBasedGetterUtils {
    private val application: Application = ApplicationManager.getApplication

    def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

    def getRiddlIdeaStates: RiddlIdeaSettings.States =
      application
        .getService(
          classOf[RiddlIdeaSettings]
        )
        .getState

    def getRiddlIdeaState(numWindow: Int): RiddlIdeaSettings.State =
      getRiddlIdeaStates.getState(numWindow)
  }

  object CreationUtils {
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

  def displayNotification(text: String): Unit = Notifications.Bus.notify(
    new Notification(
      "Riddl Plugin Notification",
      text,
      NotificationType.INFORMATION
    )
  )

  def editorForError(
      numWindow: Int,
      fileName: String,
      lineNumber: Int,
      charNumber: Int
  ): Editor = {
    val pathToConf = getRiddlIdeaState(numWindow).getConfPath
      .getOrElse("")
      .split("/")
      .dropRight(1)
      .mkString("/")

    val file: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        s"$pathToConf/$fileName"
      )
    FileEditorManager
      .getInstance(getProject)
      .openTextEditor(
        new OpenFileDescriptor(
          getProject,
          file,
          lineNumber - 1,
          charNumber - 1
        ),
        true
      )
  }

  def highlightErrorForFile(
      state: RiddlIdeaSettings.State,
      fileName: String
  ): Unit = state.getRunOutput
    .flatMap(
      _.split("\n\n")
        .filter(outputBlock => outputBlock.contains(fileName))
        .map(_.split("\n").toSeq)
    )
    .foreach(block =>
      riddlErrorRegex.findFirstMatchIn(block.head) match
        case Some(resultMatch: Regex.Match) =>
          highlightForErrorMessage(state, block, Left(resultMatch))
        case _ => ()
    )

  def highlightForErrorMessage(
      state: RiddlIdeaSettings.State,
      outputBlock: Seq[String],
      matchOrAtLine: Either[Regex.Match, Messages.Message]
  ): Unit = {
    val severity = matchOrAtLine match {
      case Left(m) => m.group(1)
      case Right(m) =>
        m.kind.severity match {
          case s if s < 4 => "[warn]"
          case _          => "[error]"
        }
    }
    val fileNameOrPath: String = matchOrAtLine match {
      case Left(m) => m.group(2)
      case Right(msg) =>
        if state.getTopLevelPath.isEmpty then ""
        else
          Path
            .of(state.getTopLevelPath.get)
            .getParent
            .toString + "/" + msg.loc.source.origin
    }
    if fileNameOrPath == "" then
      displayNotification(
        ".riddl file for editor parsing was not set"
      )
      updateToolWindowRunPane(state.getWindowNum)
      return

    val lineNumber = matchOrAtLine match {
      case Left(m)    => m.group(3).toInt - 1
      case Right(msg) => msg.loc.line
    }
    val col = matchOrAtLine match {
      case Left(m)    => m.group(3).toInt - 1
      case Right(msg) => msg.loc.col
    }
    val editor: Editor = matchOrAtLine match {
      case Left(_) =>
        editorForError(
          state.getWindowNum,
          fileNameOrPath,
          lineNumber,
          col
        )
      case Right(_) =>
        FileEditorManager
          .getInstance(getProject)
          .getSelectedTextEditor()
    }
    val markupModel: MarkupModel = editor.getMarkupModel
    state.setMarkupModel(markupModel)

    Thread.sleep(500)

    val msg = outputBlock match {
      case outBlock if outBlock.nonEmpty => outBlock.tail.mkString("\n")
      case Seq() =>
        matchOrAtLine match {
          case Right(msg) => msg.format
          case Left(_)    => ""
        }
    }

    if severity == "[error]"
    then
      val highlighter = markupModel.addLineHighlighter(
        lineNumber,
        HighlighterLayer.ERROR,
        new TextAttributes()
      )
      highlighter.setErrorStripeMarkColor(
        UIUtil.getErrorForeground
      )
      highlighter.setErrorStripeTooltip(msg)
      state.appendErrorHighlighter(highlighter)
    else if severity == "[warn]"
    then
      val highlighter = markupModel.addLineHighlighter(
        lineNumber,
        HighlighterLayer.WARNING,
        new TextAttributes()
      )
      highlighter.setErrorStripeMarkColor(
        UIUtil.getToolTipForeground
      )
      highlighter.setErrorStripeTooltip(msg)
      state.appendErrorHighlighter(highlighter)
  }

  def riddlErrorRegex: Regex =
    """(\[\w+\]) ([\w/_-]+\.riddl)\((\d+):(\d+)\)\:""".r

  def readFromOptionsFromConf(path: String): Seq[String] =
    ConfigSource.file(path).load[ConfigObject] match {
      case Right(configObject) =>
        configObject.keySet().iterator().asScala.toSeq
      case Left(err) =>
        Seq()
    }
}
