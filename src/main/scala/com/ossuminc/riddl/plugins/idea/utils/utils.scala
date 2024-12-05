package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
  MarkupModel,
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
import com.ossuminc.riddl.plugins.idea.settings.{
  HighlighterInfo,
  RiddlIdeaSettings
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.{
  getProject,
  getRiddlIdeaState
}
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

  def editorForError(
      numWindow: Int,
      fileName: String
  ): Option[Editor] = {
    val pathToFolderWithConf = getRiddlIdeaState(numWindow).getConfPath
      .getOrElse("")
      .split("/")
      .dropRight(1)
      .mkString("/")

    val file: VirtualFile =
      LocalFileSystem.getInstance.findFileByPath(
        s"$pathToFolderWithConf/$fileName"
      )

    val editor = selectedEditor
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

  def highlightErrorMessagesForFile(
      state: RiddlIdeaSettings.State,
      fileNameOrEditor: Either[Editor, String],
      forConsole: Boolean = false
  ): Unit = {
    val fileName: String = fileNameOrEditor match {
      case Right(fName) => fName
      case Left(editor) =>
        val path = Path.of(editor.getVirtualFile.getPath)
        path.iterator().asScala.toSeq.takeRight(2).mkString("/")
    }

    state
      .getHighlightersForFile(fileName)
      .foreach { highlighterInfo =>
        EditorFactory
          .getInstance()
          .getAllEditors
          .find(editor =>
            isFilePathBelowAnother(
              editor.getVirtualFile.getPath,
              state.getConfPath
            )
          )
          .foreach { editor =>
            editor.getMarkupModel.getAllHighlighters
              .find { highlighter =>
                highlighter.getStartOffset == highlighterInfo.startOffset &&
                highlighter.getEndOffset == highlighterInfo.endOffset &&
                highlighter.getLayer == highlighterInfo.layer
              }
              .foreach(rangeHighlighter =>
                rangeHighlighter.setErrorStripeMarkColor(null)
                rangeHighlighter.setErrorStripeTooltip(null)
                editor.getMarkupModel.removeHighlighter(rangeHighlighter)
              )
          }
      }
    state.clearHighlightersForFile(fileName)

    (if forConsole then state.getMessagesForConsole
     else state.getMessagesForEditor).foreach { msg =>
      val thing = fileNameOrEditor match {
        case Right(fileName) =>
          (
            editorForError(state.getWindowNum, fileName),
            Some(fileName)
          )
        case Left(editor) =>
          (
            Some(editor),
            state.getConfPath
              .map { topPath =>
                val pathFromRoot: Seq[String] =
                  editor.getVirtualFile.getPath
                    .split(
                      Path.of(topPath).getParent.toString
                    )
                    .toSeq
                if pathFromRoot.nonEmpty && pathFromRoot.length < 2 then
                  pathFromRoot(1)
                else topPath
              }
          )
      }
      println(thing)
      thing match {
        case (Some(editor), Some(fileNameFromRoot)) =>
          val markupModel: MarkupModel = editor.getMarkupModel
          val highlighter: RangeHighlighter =
            markupModel.addLineHighlighter(
              msg.loc.line,
              HighlighterLayer.WARNING,
              new TextAttributes()
            )
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
              msg.format
            )
          else if msg.kind.severity == Warning.severity then
            highlighter.setErrorStripeMarkColor(
              UIUtil.getToolTipForeground
            )
            highlighter.setErrorStripeTooltip(
              msg.format
            )
          state.saveHighlighterForFile(fileNameFromRoot, highlighter)
        case _ => ()
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
