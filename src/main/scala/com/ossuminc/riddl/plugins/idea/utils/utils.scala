package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
  HighlighterTargetArea,
  RangeHighlighter,
  TextAttributes
}
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.util.ui.UIUtil
import com.ossuminc.riddl.language.Messages
import com.ossuminc.riddl.language.Messages.{Error, Warning}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getProject
import com.typesafe.config.ConfigObject
import pureconfig.ConfigSource

import scala.jdk.CollectionConverters.*
import com.intellij.openapi.fileEditor.FileDocumentManager

import java.awt.GridBagConstraints
import java.nio.file.Path
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

  def getCurrentEditorForFile(
      filePath: String
  ): Option[Editor] = {
    val file: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        filePath
      )

    val editor = selectedEditor
    if editor != null && file != null then
      val editorFile =
        FileDocumentManager.getInstance().getFile(editor.getDocument)
      if editorFile != null && file.getName == editorFile.getName then
        Some(editor)
      else None
    else None
  }

  def selectedEditor: Editor = FileEditorManager
    .getInstance(getProject)
    .getSelectedTextEditor

  def clearHighlightersForFile(
      filePath: String,
      state: RiddlIdeaSettings.State
  ): Unit = {
    EditorFactory
      .getInstance()
      .getAllEditors
      .filter(_.getVirtualFile != null)
      .find { editor =>
        val editorFilePath = editor.getVirtualFile.getPath
        editorFilePath != null && editorFilePath == filePath && (
          isFilePathBelowAnother(
            editorFilePath,
            state.getConfPath
          ) || isFilePathBelowAnother(
            editorFilePath,
            state.getTopLevelPath
          )
        )
      }
      .foreach { editor =>
        editor.getMarkupModel.getAllHighlighters
          .filter(hl =>
            hl.getTargetArea == HighlighterTargetArea.LINES_IN_RANGE
          )
          .foreach(rangeHighlighter =>
            rangeHighlighter.setErrorStripeMarkColor(null)
            rangeHighlighter.setErrorStripeTooltip(null)
            editor.getMarkupModel.removeHighlighter(rangeHighlighter)
          )
      }
    state.clearHighlightersForFile(filePath)
  }

  def highlightErrorMessagesForFile(
      state: RiddlIdeaSettings.State,
      filePathOrEditor: Either[Editor, String],
      forConsole: Boolean = false
  ): Unit = {
    val (filePath, editorOpt): (String, Option[Editor]) =
      filePathOrEditor match {
        case Right(fPath) =>
          (fPath, getCurrentEditorForFile(fPath))
        case Left(editor) =>
          (
            editor.getVirtualFile.getPath,
            Some(editor)
          )
      }

    editorOpt.foreach { editor =>
      clearHighlightersForFile(filePath, state)
      state.clearHighlightersForFile(filePath)

      (if forConsole then state.getMessagesForConsole
       else state.getMessagesForEditor)
        .find((msgFileName, _) =>
          editor.getVirtualFile.getPath.endsWith(msgFileName)
        )
        .foreach { (_, msgs) =>
          msgs
            .foreach { msg =>
              val highlighter: RangeHighlighter =
                if msg.kind.severity == Error.severity
                then {
                  val hl: RangeHighlighter =
                    editor.getMarkupModel.addLineHighlighter(
                      msg.loc.line - 1,
                      HighlighterLayer.ERROR,
                      new TextAttributes()
                    )
                  hl.setErrorStripeMarkColor(
                    UIUtil.getErrorForeground
                  )
                  hl.setErrorStripeTooltip(
                    msg.format
                  )
                  hl
                } else if msg.kind.severity == Warning.severity then {
                  val hl: RangeHighlighter =
                    editor.getMarkupModel.addLineHighlighter(
                      msg.loc.line - 1,
                      HighlighterLayer.WARNING,
                      new TextAttributes()
                    )
                  hl.setErrorStripeMarkColor(
                    UIUtil.getToolTipForeground
                  )
                  hl.setErrorStripeTooltip(
                    msg.format
                  )
                  hl
                } else {
                  val hl: RangeHighlighter =
                    editor.getMarkupModel.addLineHighlighter(
                      msg.loc.line - 1,
                      HighlighterLayer.SYNTAX,
                      new TextAttributes()
                    )
                  hl.setErrorStripeMarkColor(
                    UIUtil.getToolTipForeground
                  )
                  hl.setErrorStripeTooltip(
                    msg.format
                  )
                  hl
                }
              state.saveHighlighterForFile(filePath, highlighter)
            }
        }
    }
  }

  def isFilePathBelowAnother(
      filePath: String,
      otherPath: Option[String]
  ): Boolean = otherPath.exists(path =>
    filePath.startsWith(
      Path.of(path).getParent.toString
    )
  )

  def readFromOptionsFromConf(path: String): Seq[String] = ConfigSource
    .file(path)
    .load[ConfigObject] match {
    case Right(configObject) =>
      configObject.keySet().iterator().asScala.toSeq
    case Left(_) =>
      Seq()
  }
}
