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
import com.ossuminc.riddl.plugins.idea.ui.{
  RiddlTerminalConsole,
  RiddlToolWindowContent
}
import ParsingUtils.*
import com.intellij.ui.components.JBPanel
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.execution.ui.ConsoleViewContentType

import scala.jdk.CollectionConverters.*
import java.awt.GridBagConstraints
import java.io.File
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
      updateToolWindowRunPane(windowNumber)
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
                genUpdateRunPaneName(windowNumber),
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
    def setConsoleProps(console: RiddlTerminalConsole): Unit = {
      console.setAutoscrolls(true)
      console.setFocusable(true)
      console.setEnabled(false)
      console.setBorder(new EmptyBorder(10, 10, 10, 10))
    }

    val state: RiddlIdeaSettings.State = getRiddlIdeaState(numWindow)

    val notConfiguredMessage: String =
      "project's configuration file not configured in settings"
    val console: RiddlTerminalConsole =
      new RiddlTerminalConsole(numWindow, project)
    setConsoleProps(console)

    def putUpdateRunPaneAsClientProperty(): Unit =
      contentPanel.putClientProperty(
        genUpdateRunPaneName(numWindow),
        (fromReload: Boolean) => updateRunPane(fromReload)
      )

    if contentPanel.getClientProperty(genUpdateRunPaneName) != null
    then contentPanel.putClientProperty(genUpdateRunPaneName, null)
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

    def updateRunPane(
        fromReload: Boolean = false
    ): Unit = {
      def writeToConsole(s: String, clear: Boolean = true): Unit = {
        if clear then console.clear()
        setConsoleProps(console)
        console.print(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
      }

      def writeStateOutputToConsole(): Unit = {
        console.clear()
        console.printMessages()
      }

      def highlightMessages(): Unit =
        if isFilePathBelowAnother(
            selectedEditor.getVirtualFile.getPath,
            state.getConfPath
          )
        then highlightErrorMessagesForFile(state, Left(selectedEditor), true)

      val tabContent: Content = getToolWindowContent(numWindow)
      if tabContent.getTabName.isBlank then
        tabContent.setDisplayName(genWindowName(numWindow))

      val statePath: String =
        if state != null then state.getConfPath.getOrElse("")
        else ""
      val confFile = File(statePath)

      if state != null && state.getCommand == "from" then
        if statePath == null || statePath.isBlank then
          writeToConsole(notConfiguredMessage)
        else if state.getFromOption.isEmpty then
          writeToConsole("From command chosen, but no option has been chosen")
        else if confFile.exists() && confFile.isFile then
          if state.getMessagesForConsole.nonEmpty
          then
            writeStateOutputToConsole()
            highlightMessages()
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

  private def getToolWindowContent(numWindow: Int): Content =
    getContentManager.getContents
      .find(_.getTabName == genWindowName(numWindow))
      .getOrElse(
        getContentManager.getContents
          .find(_.getTabName.count(_.isDigit) == 0)
          .getOrElse(getContentManager.getContents.head)
      )

  def updateToolWindowRunPane(
      numWindow: Int,
      fromReload: Boolean = false
  ): Unit =
    getToolWindowContent(numWindow).getComponent
      .getClientProperty(genUpdateRunPaneName(numWindow))
      .asInstanceOf[(fromReload: Boolean) => Unit](
        fromReload
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
    if windowNumber > 1 then
      s"RIDDL ${getRiddlIdeaState(windowNumber).getCommand} - (Window #$windowNumber)"
    else s"RIDDL ${getRiddlIdeaState(1).getCommand}"
  }

  private def genUpdateRunPaneName(numWindow: Int) =
    s"updateRunPane_$numWindow"
}
