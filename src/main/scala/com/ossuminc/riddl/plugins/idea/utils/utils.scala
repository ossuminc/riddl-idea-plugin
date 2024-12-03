package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.editor.markup.{HighlighterLayer, MarkupModel, TextAttributes}
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.util.ui.UIUtil
import com.ossuminc.riddl.language.Messages
import com.ossuminc.riddl.language.Messages.{Error, Warning}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaState
}
import com.typesafe.config.ConfigObject
import pureconfig.ConfigSource

import scala.jdk.CollectionConverters.*
import com.intellij.openapi.fileEditor.FileDocumentManager

import java.awt.GridBagConstraints
import javax.swing.Icon

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
      fileName: String
  ): Editor = {
    val pathToFolderWithConf = getRiddlIdeaState(numWindow).getConfPath
      .getOrElse("")
      .split("/")
      .dropRight(1)
      .mkString("/")

    val file: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        s"$pathToFolderWithConf/$fileName"
      )

    val fileEditorManager = FileEditorManager
      .getInstance(getProject)
    val editor = fileEditorManager.getSelectedTextEditor

    if editor != null && file != null then
      val virtualFile =
        FileDocumentManager.getInstance().getFile(editor.getDocument)
      if virtualFile != null && file.getName == virtualFile.getName then
        Some(editor)
      else None
    else None
  }

  private def selectedEditor: Editor = {
    val fileEditorManager = FileEditorManager
      .getInstance(getProject)
    val editor = fileEditorManager.getSelectedTextEditor

    editor
  }

  def highlightForErrorMessage(
      state: RiddlIdeaSettings.State,
      fileName: String
  ): Unit = state.getMessages match {
    case empty if empty.isEmpty =>
      state
        .getHighlightersForFile(fileName)
        .foreach(hlInfo =>
          EditorFactory
            .getInstance()
            .getAllEditors
            .head
            .getMarkupModel
            .getAllHighlighters
            .find { highlighter =>
              highlighter.getStartOffset == hlInfo.startOffset &&
              highlighter.getEndOffset == hlInfo.endOffset &&
              highlighter.getLayer == hlInfo.layer
            }
            .foreach(hl =>
              hl.setErrorStripeMarkColor(null)
              hl.setErrorStripeTooltip(null)
              selectedEditor.getMarkupModel.removeHighlighter(hl)
            )
        )
      state.clearHighlightersForFile(fileName)
    case output =>
      output.foreach { msg =>
        val editor = editorForError(
          state.getWindowNum,
          fileName
        )

        if editor.isDefined then
          val markupModel = editor.get.getMarkupModel
          if msg.kind.severity == Error.severity then
            val highlighter = markupModel.addLineHighlighter(
              msg.loc.line,
              HighlighterLayer.ERROR,
              new TextAttributes()
            )
            highlighter.setErrorStripeMarkColor(
              UIUtil.getErrorForeground
            )
            highlighter.setErrorStripeTooltip(
              msg.message
            )
            println((fileName, highlighter))
            state.saveHighlighterForFile(fileName, highlighter)
          else if msg.kind.severity == Warning.severity then
            val highlighter = markupModel.addLineHighlighter(
              msg.loc.line,
              HighlighterLayer.WARNING,
              new TextAttributes()
            )
            highlighter.setErrorStripeMarkColor(
              UIUtil.getToolTipForeground
            )
            highlighter.setErrorStripeTooltip(
              msg.message
            )
            state.saveHighlighterForFile(fileName, highlighter)
      }
      state.clearHighlightersForFile(fileName)
  }
}

def readFromOptionsFromConf(path: String): Seq[String] = ConfigSource
  .file(path)
  .load[ConfigObject] match {
  case Right(configObject) =>
    configObject.keySet().iterator().asScala.toSeq
  case Left(err) =>
    Seq()
}
