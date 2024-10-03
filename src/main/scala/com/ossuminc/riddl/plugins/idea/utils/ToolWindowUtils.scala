package com.ossuminc.riddl.plugins.idea.utils

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.ui.content.{
  Content,
  ContentFactory,
  ContentManager,
  ContentManagerEvent,
  ContentManagerListener
}
import com.ossuminc.riddl.plugins.idea.settings.{
  RiddlIdeaSettings,
  RiddlIdeaSettingsConfigurable
}
import com.ossuminc.riddl.plugins.idea.ui.RiddlToolWindowContent
import ParsingUtils.*
import CreationUtils.*
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ui.components.JBPanel
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.process.OSProcessHandler
import com.intellij.terminal.TerminalExecutionConsole

import scala.jdk.CollectionConverters.*
import java.awt.GridBagConstraints
import java.io.File
import javax.swing.border.EmptyBorder

object ToolWindowUtils {
  import ManagerBasedGetterUtils.*

  implicit class ToolWindowExt(toolWindow: ToolWindow) {
    def createAndAddContentToTW(
        project: Project,
        windowNumber: Int,
        isLockable: Boolean = false
    ): Unit = {
      val windowName: String = genWindowName(windowNumber)

      val content = ContentFactory
        .getInstance()
        .createContent(
          new RiddlToolWindowContent(
            toolWindow,
            project,
            windowNumber
          ).getContentPanel,
          windowName,
          isLockable
        )
      content.setCloseable(!isLockable)
      listenForContentRemoval(project, windowName, windowNumber)
      getContentManager.addContent(content)
      updateToolWindowPanes(windowNumber)
    }

    private def listenForContentRemoval(
        project: Project,
        windowDisplayName: String,
        windowNum: Int
    ): Unit = {
      val riddlContentManager = ToolWindowManager
        .getInstance(project)
        .getToolWindow("riddl")
        .getContentManager

      riddlContentManager.addContentManagerListener(
        new ContentManagerListener() {
          private val windowName: String = windowDisplayName
          private val windowNumber: Int = windowNum

          override def contentRemoved(event: ContentManagerEvent): Unit = {
            val content = event.getContent

            if event.getContent.getDisplayName == windowName then {
              content.getComponent.putClientProperty(
                genUpdateRunPaneLabelName(windowNumber),
                null
              )
              riddlContentManager.removeContentManagerListener(this)
              getRiddlIdeaStates.removeState(windowNumber)
            }
          }
        }
      )
    }
  }

  def createRunConsole(
      toolWindow: ToolWindow,
      contentPanel: JBPanel[?],
      numWindow: Int,
      project: Project
  ): Unit = {
    def setConsoleProps(console: TerminalExecutionConsole): Unit = {
      console.getTerminalWidget.setAutoscrolls(true)
      console.getTerminalWidget.getTerminalPanel.setCursorVisible(false)
      console.getTerminalWidget.setFocusable(true)
      console.getTerminalWidget.setEnabled(false)
      console.getTerminalWidget.setBorder(new EmptyBorder(10, 10, 10, 10))
    }

    val state: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    val notConfiguredMessage: String =
      "project's .conf file not configured in settings"
    val processHandler = new OSProcessHandler(new GeneralCommandLine("echo"))
    val console: TerminalExecutionConsole =
      new TerminalExecutionConsole(project, processHandler)
    setConsoleProps(console)

    def putUpdateRunPaneLabelAsClientProperty(): Unit =
      contentPanel.putClientProperty(
        genUpdateRunPaneLabelName(numWindow),
        (fromReload: Boolean, fromLogger: Boolean) =>
          updateRunPaneLabel(fromReload, fromLogger)
      )

    if contentPanel.getClientProperty(genUpdateRunPaneLabelName) != null
    then contentPanel.putClientProperty(genUpdateRunPaneLabelName, null)
    putUpdateRunPaneLabelAsClientProperty()

    if contentPanel.getClientProperty("createToolWindow") == null then
      contentPanel.putClientProperty(
        "createToolWindow",
        () => createToolWindow()
      )

    def createToolWindow(): Unit =
      toolWindow.createAndAddContentToTW(
        project,
        getRiddlIdeaStates.newState()
      )

    def updateRunPaneLabel(
        fromReload: Boolean = false,
        fromLogger: Boolean = false
    ): Unit = {
      def writeToConsole(s: String): Unit = {
        console.getTerminalWidget.getTerminal.reset(true)
        setConsoleProps(console)
        console.print(s, ConsoleViewContentType.SYSTEM_OUTPUT)
      }

      def writeStateOutputToConsole(): Unit = writeToConsole(
        state.getRunOutput.mkString("\n")
      )

      if fromLogger then
        writeStateOutputToConsole()
        return

      val statePath: String =
        if state != null then state.getConfPath
        else ""

      val confFile = File(statePath)

      val tabContent: Content = getToolWindowContent(numWindow)
      val expectedName = genWindowName(numWindow)

      if tabContent.getTabName != expectedName then
        tabContent.setDisplayName(expectedName)

      if state.getCommand == "from" && (statePath == null || statePath.isBlank)
      then
        writeToConsole(notConfiguredMessage)
        return

      if state.getRunOutput.nonEmpty then writeStateOutputToConsole()
      else if state.getCommand == "from" then
        if confFile.exists() && confFile.isFile then
          runCommandForWindow(numWindow, Some(statePath))
        else
          writeToConsole(
            s"This window's .conf file:\n  " + statePath + "\nwas not found, please configure it in settings"
          )
      else if fromReload then runCommandForWindow(numWindow)
    }

    // enables auto-compiling
    project.getMessageBus
      .connect()
      .subscribe(
        VirtualFileManager.VFS_CHANGES,
        new BulkFileListener {
          override def after(events: java.util.List[? <: VFileEvent]): Unit = {
            if events.asScala.toSeq.exists(_.isFromSave) && state.getAutoCompile
            then {
              state.clearRunOutput()
              runCommandForWindow(numWindow, Some(state.getConfPath))
            }
          }
        }
      )

    contentPanel.add(
      console.getComponent,
      createGBCs(1, 0, 1, 1, GridBagConstraints.BOTH)
    )
  }

  private def getContentManager: ContentManager = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")
    .getContentManager

  private def getToolWindowContent(numWindow: Int): Content =
    getContentManager.getContents
      .find(_.getTabName.contains(numWindow.toString))
      .getOrElse(
        getContentManager.getContents
          .find(_.getTabName.count(_.isDigit) == 0)
          .getOrElse(getContentManager.getContents.head)
      )

  def updateToolWindowPanes(
      numWindow: Int,
      fromReload: Boolean = false,
      fromLogger: Boolean = false
  ): Unit =
    updateToolWindowRunPane(numWindow, fromReload, fromLogger)

  def updateToolWindowRunPane(
      numWindow: Int,
      fromReload: Boolean = false,
      fromLogger: Boolean = false
  ): Unit =
    getToolWindowContent(numWindow).getComponent
      .getClientProperty(genUpdateRunPaneLabelName(numWindow))
      .asInstanceOf[(fromReload: Boolean, fromLogger: Boolean) => Unit](
        fromReload,
        fromLogger
      )

  def createNewToolWindow(): Unit =
    getToolWindowContent(0).getComponent
      .getClientProperty("createToolWindow")
      .asInstanceOf[() => Unit]()

  def openToolWindowSettings(numWindow: Int): Unit =
    ShowSettingsUtil.getInstance
      .editConfigurable(
        getProject,
        new RiddlIdeaSettingsConfigurable(numWindow)
      )

  private def genWindowName(windowNumber: Int): String = {
    val windowNumInName =
      if windowNumber > 1 then s" - (Window #$windowNumber)" else ""
    s"RIDDL ${getRiddlIdeaState(windowNumber).getCommand}$windowNumInName"
  }

  private def genUpdateRunPaneLabelName(numWindow: Int) =
    s"updateRunPaneLabel_$numWindow"
}
