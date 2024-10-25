package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.{HighlighterLayer, TextAttributes}
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.util.ui.UIUtil
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaState
}
import com.intellij.openapi.editor.markup.MarkupModel

import java.awt.GridBagConstraints
import javax.swing.Icon
import scala.util.matching.Regex

package object utils {
  def RiddlIcon[T <: Class[?]](classType: T): Icon =
    IconLoader.getIcon("images/RIDDL-icon.jpg", classType)

  object ManagerBasedGetterUtils {
    val application: Application = ApplicationManager.getApplication

    def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

    def getRiddlIdeaStates: RiddlIdeaSettings.States =
      application
        .getService(
          classOf[RiddlIdeaSettings]
        )
        .getState

    def getRiddlIdeaState(numToolWindow: Int): RiddlIdeaSettings.State =
      getRiddlIdeaStates.getState(numToolWindow)
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
    .foreach(block => highlightErrorBlock(state, block))

  def highlightErrorBlock(
      state: RiddlIdeaSettings.State,
      outputBlock: Seq[String]
  ): Unit =
    riddlErrorRegex.findFirstMatchIn(outputBlock.head) match
      case Some(resultMatch: Regex.Match) =>
        val editor = editorForError(
          state.getWindowNum,
          resultMatch.group(2),
          resultMatch.group(3).toInt,
          resultMatch.group(4).toInt
        )

        val markupModel: MarkupModel = editor.getMarkupModel
        Thread.sleep(500)

        if resultMatch.group(1) == "[error]" then
          val highlighter = markupModel.addLineHighlighter(
            resultMatch.group(3).toInt,
            HighlighterLayer.ERROR,
            new TextAttributes()
          )
          highlighter.setErrorStripeMarkColor(
            UIUtil.getErrorForeground
          )
          highlighter.setErrorStripeTooltip(outputBlock.tail.mkString("\n"))
        else if resultMatch.group(1) == "[warn]" then
          val highlighter = markupModel.addLineHighlighter(
            resultMatch.group(3).toInt,
            HighlighterLayer.WARNING,
            new TextAttributes()
          )
          highlighter.setErrorStripeMarkColor(
            UIUtil.getToolTipForeground
          )
          highlighter.setErrorStripeTooltip(outputBlock.tail.mkString("\n"))
      case _ => ()

  def riddlErrorRegex: Regex =
    """(\[\w+\]) ([\w/_-]+\.riddl)\((\d+):(\d+)\)\:""".r
}
