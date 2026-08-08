/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
import com.ossuminc.riddl.plugins.idea.settings.{RiddlIdeaSettings, RiddlIdeaSettingsConfigurable}
import com.ossuminc.riddl.plugins.idea.ui.{RiddlTerminalConsole, RiddlToolWindowContent}
import ParsingUtils.*
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ui.components.JBPanel
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.actionSystem.{
  ActionManager,
  ActionPlaces,
  ActionPopupMenu,
  AnAction,
  DefaultActionGroup
}
import com.intellij.terminal.{TerminalExecutionConsole, TerminalExecutionConsoleBuilder}
import com.ossuminc.riddl.plugins.idea.actions.{EditTabNameAction, RiddlActionGroup}

import scala.jdk.CollectionConverters.*
import java.awt.GridBagConstraints
import java.awt.event.{MouseAdapter, MouseEvent}
import java.io.File
import javax.swing.{JComponent, JMenuItem, JPopupMenu}
import javax.swing.border.EmptyBorder

object ToolWindowUtils {
  import ManagerBasedGetterUtils.*
  import CreationUtils.createGBCs

  implicit class ToolWindowExt(toolWindow: ToolWindow) {
    def createAndAddContentToTW(
      project: Project,
      windowNumber: Int,
      isLockable: Boolean = false
    ): Unit = {
      val windowName: String = genWindowName(windowNumber)

      // Use the ToolWindow the platform handed us instead of looking it up by
      // id. This runs inside ToolWindowFactory.createToolWindowContent, which
      // the manager calls from doShowWindow -> createContentIfNeeded *before*
      // the window is answerable by id, so
      // ToolWindowManager.getInstance(project).getToolWindow("riddl") returns
      // null here and anything dereferencing it NPEs. That crash left the pane
      // blank because it aborted before addContent. See NOTEBOOK.md
      // "Tool window NPE" (2026-08-08).
      val contentManager = toolWindow.getContentManager

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
      listenForContentRemoval(contentManager, windowName, windowNumber)
      contentManager.addContent(content)
      // Update through the Content we just created rather than re-finding it
      // by number, which would go through the same by-id lookup.
      updateRunPaneOf(content, windowNumber)
    }

    private def listenForContentRemoval(
      riddlContentManager: ContentManager,
      windowDisplayName: String,
      windowNum: Int
    ): Unit = {
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
              getRiddlIdeaState(windowNum).disconnectVFSListener()
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
      val terminalWidget = console.getTerminalWidget
      terminalWidget.setAutoscrolls(true)
      terminalWidget.getTerminalPanel.setCursorVisible(false)
      terminalWidget.setFocusable(true)
      terminalWidget.setEnabled(false)
      terminalWidget.setBorder(new EmptyBorder(10, 10, 10, 10))
    }

    val state: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    val notConfiguredMessage: String =
      "project's configuration file not configured in settings"
    val processHandler = new OSProcessHandler(new GeneralCommandLine("echo"))
    // The (Project, ProcessHandler) constructor is deprecated as of 2026.1;
    // the builder plus an explicit attachToProcess is the sanctioned form and
    // has identical semantics -- the old constructor simply called
    // attachToProcess itself when the handler was non-null.
    val console: TerminalExecutionConsole =
      new TerminalExecutionConsoleBuilder(project).build()
    console.attachToProcess(processHandler)
    setConsoleProps(console)

    def putUpdateRunPaneAsClientProperty(): Unit =
      contentPanel.putClientProperty(
        genUpdateRunPaneLabelName(numWindow),
        (fromReload: Boolean, fromLogger: Boolean) => updateRunPaneLabel(fromReload)
      )

    if contentPanel.getClientProperty(genUpdateRunPaneLabelName) != null
    then contentPanel.putClientProperty(genUpdateRunPaneLabelName, null)
    putUpdateRunPaneAsClientProperty()

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
      def writeToConsole(s: String, clear: Boolean = true): Unit = {
        console.getTerminalWidget.getTerminal.reset(true)
        if clear then console.clear()
        setConsoleProps(console)
        console.print(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
      }

      def writeStateOutputToConsole(): Unit = writeToConsole(
        state.getRunOutput.mkString("\n")
      )

      if fromLogger then
        writeStateOutputToConsole()
        return

      val statePath: String =
        if state != null then state.getConfPath.getOrElse("")
        else ""

      val confFile = File(statePath)

      // Resolve through the ToolWindow in scope, not by id: this runs during
      // tool window creation, when the id lookup still returns null.
      val tabContent: Content =
        contentOf(toolWindow.getContentManager, numWindow)
      if tabContent.getTabName.isBlank then tabContent.setDisplayName(genWindowName(numWindow))
      tabContent.getComponent.addMouseListener(new MouseAdapter {
        override def mousePressed(e: MouseEvent): Unit = {
          if e.isPopupTrigger then showPopup(e)
        }

        override def mouseReleased(e: MouseEvent): Unit = {
          if e.isPopupTrigger then showPopup(e)
        }

        private def showPopup(e: MouseEvent): Unit = {
          val actionManager = ActionManager.getInstance()
          val actionGroup: RiddlActionGroup = RiddlActionGroup()
          val anAction: AnAction = actionManager.getAction("EditTabNameAction")

          val actionPopupMenu: ActionPopupMenu = ActionManager
            .getInstance()
            .createActionPopupMenu(ActionPlaces.UNKNOWN, actionGroup)
          val component: JComponent = e.getComponent.asInstanceOf[JComponent]
          val popupMenu: JPopupMenu = actionPopupMenu.getComponent
          popupMenu.show(component, e.getX, e.getY)
        }
      })

      if state.getCommand == "from" && (statePath == null || statePath.isBlank)
      then
        writeToConsole(notConfiguredMessage)
        return

      if state.getRunOutput.nonEmpty then writeStateOutputToConsole()
      else if state.getCommand == "from" then
        if confFile.exists() && confFile.isFile then runCommandForConsole(numWindow)
      if state != null && state.getCommand == "from" then
        if statePath == null || statePath.isBlank then writeToConsole(notConfiguredMessage)
        else if state.getFromOption.isEmpty then
          writeToConsole("From command chosen, but no option has been chosen")
        else if confFile.exists() && confFile.isFile then
          if state.getMessagesForConsole.nonEmpty
          then writeStateOutputToConsole()
          else runCommandForConsole(numWindow)
        else
          writeToConsole(
            s"This window's configuration file:\n  " + statePath + "\nwas not found, please configure it in settings"
          )
      else if state != null then
        if state.getRunOutput.nonEmpty then
          console.clear()
          state.getRunOutput.foreach(msg => writeToConsole(msg, false))
        else if fromReload then runCommandForConsole(numWindow)
    }

    // enables auto-compiling
    val connection = project.getMessageBus
      .connect()
    state.setVFSConnection(connection)

    connection
      .subscribe(
        VirtualFileManager.VFS_CHANGES,
        new BulkFileListener {
          override def after(events: java.util.List[? <: VFileEvent]): Unit = {
            if events.asScala.toSeq.exists(
                _.isFromSave
              ) && state.getAutoParse && state.getCommand == "from"
            then
              state.clearRunOutput()
              runCommandForConsole(numWindow)
              updateToolWindowRunPane(numWindow, fromReload = true)
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

  def getToolWindowContent(numWindow: Int): Content =
    contentOf(getContentManager, numWindow)

  /** Find a window's [[Content]] in a caller-supplied [[ContentManager]].
    *
    * Callers running while the tool window is still being created must pass
    * `toolWindow.getContentManager`; [[getToolWindowContent]] resolves the manager by tool-window
    * id, which is not answerable until creation is finished.
    */
  def contentOf(contentManager: ContentManager, numWindow: Int): Content =
    contentManager.getContents
      .find(_.getTabName == genWindowName(numWindow))
      .getOrElse(
        contentManager.getContents
          .find(_.getTabName.count(_.isDigit) == 0)
          .getOrElse(contentManager.getContents.head)
      )

  def updateToolWindowPanes(
    numWindow: Int,
    fromReload: Boolean = false,
    fromLogger: Boolean = false
  ): Unit = updateToolWindowRunPane(numWindow, fromReload, fromLogger)

  def updateToolWindowRunPane(
    numWindow: Int,
    fromReload: Boolean = false,
    fromLogger: Boolean = false
  ): Unit =
    updateRunPaneOf(getToolWindowContent(numWindow), numWindow, fromReload, fromLogger)

  /** Update a run pane given its [[Content]] directly.
    *
    * Callers that already hold the Content must use this rather than [[updateToolWindowRunPane]],
    * which re-finds it via `ToolWindowManager.getToolWindow("riddl")` -- a lookup that returns null
    * while the tool window is still being created.
    */
  private def updateRunPaneOf(
    content: Content,
    numWindow: Int,
    fromReload: Boolean = false,
    fromLogger: Boolean = false
  ): Unit = content.getComponent
    .getClientProperty(genUpdateRunPaneLabelName(numWindow))
    .asInstanceOf[(fromReload: Boolean, fromLogger: Boolean) => Unit](
      fromReload,
      fromLogger
    )

  def createNewToolWindow(): Unit = getToolWindowContent(1).getComponent
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

    val windowState = getRiddlIdeaState(windowNumber)
    if windowState != null then s"RIDDL ${windowState.getCommand}$windowNumInName"
    else s"RIDDL ?$windowNumInName"
  }

  private def genUpdateRunPaneLabelName(numWindow: Int) =
    s"updateRunPaneLabel_$numWindow"
}
