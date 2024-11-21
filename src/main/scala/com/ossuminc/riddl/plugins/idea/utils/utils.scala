package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.editor.markup.{HighlighterLayer, TextAttributes}
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.util.ui.UIUtil
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  HighlighterInfo
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaState
}
import com.ossuminc.riddl.plugins.idea.riddlErrorRegex
import com.typesafe.config.ConfigObject
import pureconfig.ConfigSource

import scala.jdk.CollectionConverters.*
import com.intellij.openapi.fileEditor.FileDocumentManager

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
      fileName: String
  ): Option[Editor] = {
    val pathToConf = getRiddlIdeaState(numWindow).getConfPath
      .getOrElse("")
      .split("/")
      .dropRight(1)
      .mkString("/")

    val file: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        s"$pathToConf/$fileName"
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

  def selectedEditor: Editor = {
    val fileEditorManager = FileEditorManager
      .getInstance(getProject)
    val editor = fileEditorManager.getSelectedTextEditor

    editor
  }

  def highlightErrorForFile(
      state: RiddlIdeaSettings.State,
      fileName: String
  ): Unit = state.getRunOutput match {
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
      output
        .flatMap(
          _.split("\n\n")
            .filter(outputBlock => outputBlock.contains(fileName))
            .map(_.split("\n").toSeq)
        )
        .filter(_.nonEmpty)
        .foreach { outputBlock =>
          riddlErrorRegex.findFirstMatchIn(outputBlock.head) match
            case Some(resultMatch: Regex.Match) =>
              val editor = editorForError(
                state.getWindowNum,
                resultMatch.group(2)
              )

              if editor.isDefined then
                val markupModel = editor.get.getMarkupModel
                if resultMatch.group(1) == "[error]" then
                  val highlighter = markupModel.addLineHighlighter(
                    resultMatch.group(3).toInt,
                    HighlighterLayer.ERROR,
                    new TextAttributes()
                  )
                  highlighter.setErrorStripeMarkColor(
                    UIUtil.getErrorForeground
                  )
                  highlighter.setErrorStripeTooltip(
                    outputBlock.tail.mkString("\n")
                  )
                  println((fileName, highlighter))
                  state.saveHighlighterForFile(fileName, highlighter)
                else if resultMatch.group(1) == "[warn]" then
                  val highlighter = markupModel.addLineHighlighter(
                    resultMatch.group(3).toInt,
                    HighlighterLayer.WARNING,
                    new TextAttributes()
                  )
                  highlighter.setErrorStripeMarkColor(
                    UIUtil.getToolTipForeground
                  )
                  highlighter.setErrorStripeTooltip(
                    outputBlock.tail.mkString("\n")
                  )
                  state.saveHighlighterForFile(fileName, highlighter)
            case _ =>
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
        }
  }

  def readFromOptionsFromConf(path: String): Seq[String] =
    ConfigSource.file(path).load[ConfigObject] match {
      case Right(configObject) =>
        configObject.keySet().iterator().asScala.toSeq
      case Left(err) =>
        Seq()
    }
}

def riddlErrorRegex = """(\[\w+\]) ([\w\/_-]+\.riddl)\((\d+):(\d+)\)\:""".r
