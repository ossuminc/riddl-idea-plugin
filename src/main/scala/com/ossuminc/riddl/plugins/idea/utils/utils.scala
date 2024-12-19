package com.ossuminc.riddl.plugins.idea

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.editor.markup.{
  EffectType,
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
    def removeHighlighter(
        editor: Editor
    )(rangeHighlighter: RangeHighlighter): Unit = {
      rangeHighlighter.setErrorStripeMarkColor(null)
      rangeHighlighter.setErrorStripeTooltip(null)
      editor.getMarkupModel.removeHighlighter(rangeHighlighter)
    }

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
        val allHighlighters = editor.getMarkupModel.getAllHighlighters
        val colorScheme = EditorColorsManager.getInstance().getGlobalScheme

        allHighlighters
          .filter(hl =>
            hl.getTextAttributes(colorScheme)
              .getEffectType == EffectType.WAVE_UNDERSCORE ||
              hl.getTextAttributes(colorScheme)
                .getEffectType == EffectType.
          )
          .foreach(removeHighlighter(editor))
      }
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
      // break up first function using other two
      clearHighlightersForFile(filePath, state)
      state.clearLineHighlightersForFile(filePath)
      state.clearSquigglyHighlightersForFile(filePath)

      (if forConsole then state.getMessagesForConsole
       else state.getMessagesForEditor)
        .find((msgFileName, _) =>
          editor.getVirtualFile.getPath.endsWith(msgFileName)
        )
        .foreach { (_, msgs) =>
          msgs
            .foreach { msg =>
              val squigglyAttributes = new TextAttributes()
              squigglyAttributes.setEffectType(EffectType.WAVE_UNDERSCORE)
              val squigglyStart = msg.loc.offset
              val squigglyEnd = squigglyStart + msg.loc.endOffset

              val highlighter: RangeHighlighter =
                if msg.kind.severity == Error.severity
                then {
                  squigglyAttributes.setEffectColor(UIUtil.getErrorForeground)
                  state.saveLineHighlighterForFile(
                    filePath,
                    editor.getMarkupModel.addRangeHighlighter(
                      squigglyStart,
                      squigglyEnd,
                      HighlighterLayer.ERROR,
                      squigglyAttributes,
                      HighlighterTargetArea.EXACT_RANGE
                    )
                  )

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
                  squigglyAttributes.setEffectColor(UIUtil.getToolTipForeground)
                  state.saveLineHighlighterForFile(
                    filePath,
                    editor.getMarkupModel.addRangeHighlighter(
                      squigglyStart,
                      squigglyEnd,
                      HighlighterLayer.WARNING,
                      squigglyAttributes,
                      HighlighterTargetArea.EXACT_RANGE
                    )
                  )

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
                  squigglyAttributes.setEffectColor(
                    UIUtil.getTextAreaForeground
                  )
                  state.saveSquigglyHighlighterForFile(
                    filePath,
                    editor.getMarkupModel.addRangeHighlighter(
                      squigglyStart,
                      squigglyEnd,
                      HighlighterLayer.WEAK_WARNING,
                      squigglyAttributes,
                      HighlighterTargetArea.EXACT_RANGE
                    )
                  )

                  val hl: RangeHighlighter =
                    editor.getMarkupModel.addLineHighlighter(
                      msg.loc.line - 1,
                      HighlighterLayer.WEAK_WARNING,
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
              state.saveLineHighlighterForFile(filePath, highlighter)
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
