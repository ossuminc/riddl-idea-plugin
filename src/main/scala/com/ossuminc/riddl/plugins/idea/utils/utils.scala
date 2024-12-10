package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
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
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaState
}
import com.typesafe.config.ConfigObject
import pureconfig.ConfigSource

import scala.jdk.CollectionConverters.*
import com.intellij.openapi.fileEditor.FileDocumentManager

import java.awt.GridBagConstraints
import java.io.File
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

  def editorForErroneousFile(
      numWindow: Int,
      fileName: String
  ): Option[Editor] = {
    val runPathFolder = Path
      .of(
        getRiddlIdeaState(numWindow).getConfPath
          .getOrElse(
            getRiddlIdeaState(numWindow).getTopLevelPath.getOrElse("")
          )
      )
      .getParent
      .toFile
      .getPath

    val erroneousFile: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        s"$runPathFolder/$fileName"
      )

    val editor = selectedEditor
    if editor != null && erroneousFile != null then
      val editorFile =
        FileDocumentManager.getInstance().getFile(editor.getDocument)
      if editorFile != null && erroneousFile.getName == editorFile.getName then
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

  def highlightErrorMessagesForFile(
      state: RiddlIdeaSettings.State,
      fileNameOrEditor: Either[Editor, String],
      forConsole: Boolean = false
  ): Unit = {
    val (fileName, editor): (String, Option[Editor]) = fileNameOrEditor match {
      case Right(fName) =>
        (fName, editorForErroneousFile(state.getWindowNum, fName))
      case Left(editor) =>
        (
          new File(editor.getVirtualFile.getPath).getName,
          Some(editor)
        )
    }

    state
      .getHighlightersForFile(fileName)
      .foreach { highlighterInfo =>
        EditorFactory
          .getInstance()
          .getAllEditors
          .find(editor =>
            val editorFilePath = editor.getVirtualFile.getPath
            editorFilePath != null && isFilePathBelowAnother(
              editorFilePath,
              state.getConfPath
            ) && editorFilePath.endsWith(fileName)
          )
          .foreach { editor =>
            editor.getMarkupModel.getAllHighlighters
              .find { highlighter =>
                highlighter.getStartOffset == highlighterInfo.startOffset &&
                highlighter.getEndOffset == highlighterInfo.endOffset &&
                highlighter.getLayer == highlighterInfo.layer
              }
              .foreach(rangeHighlighter =>
                println("found")
                rangeHighlighter.setErrorStripeMarkColor(null)
                rangeHighlighter.setErrorStripeTooltip(null)
                editor.getMarkupModel.removeHighlighter(rangeHighlighter)
              )
          }
      }
    state.clearHighlightersForFile(fileName)

    (if forConsole then state.getMessagesForConsole
     else state.getMessagesForEditor)
      .find((msgFileName, _) =>
        editor.exists(
          _.getVirtualFile.getPath.endsWith(msgFileName)
        )
      )
      .foreach { (msgFileName, msgs) =>
        editor.foreach { editor =>
          msgs
            .foreach { msg =>
              val highlighter: RangeHighlighter =
                if msg.kind.severity == Error.severity then {
                  val hl: RangeHighlighter =
                    editor.getMarkupModel.addLineHighlighter(
                      msg.loc.line,
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
                      msg.loc.line,
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
                      msg.loc.line,
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
              state.saveHighlighterForFile(msgFileName, highlighter)
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
