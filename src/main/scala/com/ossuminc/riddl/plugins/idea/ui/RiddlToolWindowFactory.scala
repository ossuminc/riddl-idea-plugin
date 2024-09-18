package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.actionSystem.{ActionManager, ActionPlaces, ActionToolbar, DefaultActionGroup}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.intellij.ui.content.Content
import com.ossuminc.riddl.plugins.idea.actions.{RiddlNewToolWindowAction, RiddlToolWindowCompileAction, RiddlToolWindowSettingsOpenAction}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.utils.ParsingUtils.runCommandForWindow
import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.*
import com.ossuminc.riddl.plugins.utils.*
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.utils.CreationUtils.*
import org.jdesktop.swingx.VerticalLayout

import java.awt.{GridBagConstraints, GridBagLayout}
import java.io.File
import javax.swing.border.EmptyBorder
import javax.swing.{JScrollPane, ScrollPaneConstants, ScrollPaneLayout, SwingConstants}

class RiddlToolWindowFactory extends ToolWindowFactory {
  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = toolWindow.createAndAddContentToTW(project, getRiddlIdeaStates.newState(), true)
}

class RiddlToolWindowContent(
    toolWindow: ToolWindow,
    project: Project,
    numWindow: Int
) {
  private val state: RiddlIdeaSettings.State =
    getRiddlIdeaState(numWindow)

  private val notConfiguredMessage: String =
    "project's .conf file not configured in settings"
  private val sideBar: SimpleToolWindowPanel =
    new SimpleToolWindowPanel(false, false)
  sideBar.setLayout(VerticalLayout())

  private val actionGroup: DefaultActionGroup =
    new DefaultActionGroup("com.ossuminc.riddl.plugins.idea.actions.RiddlActionsGroup", false)
  actionGroup.add(new RiddlNewToolWindowAction)
  private val compileAction: RiddlToolWindowCompileAction =
    new RiddlToolWindowCompileAction()
  compileAction.setWindowNum(numWindow)
  actionGroup.add(compileAction)
  private val openAction: RiddlToolWindowSettingsOpenAction =
    new RiddlToolWindowSettingsOpenAction
  openAction.setWindowNum(numWindow)
  actionGroup.add(openAction)

  private val actionToolbar: ActionToolbar = ActionManager
    .getInstance()
    .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, false)
  actionToolbar.setTargetComponent(sideBar)
  sideBar.setToolbar(actionToolbar.getComponent)

  private val contentPanel: JBPanel[?] = new JBPanel()
  contentPanel.setLayout(GridBagLayout())

  contentPanel.add(
    sideBar,
    createGBCs(0, 0, 0, 0, GridBagConstraints.VERTICAL)
  )

  private val outputLabel: JBLabel = new JBLabel()
  outputLabel.setText(notConfiguredMessage)
  outputLabel.setVerticalAlignment(SwingConstants.TOP)
  outputLabel.setBorder(new EmptyBorder(10, 10, 10, 10))

  private val scrollPane = new JScrollPane(
    outputLabel,
    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
  )
  scrollPane.setLayout(ScrollPaneLayout())

  def putUpdateLabelAsClientProperty(): Unit = {
    contentPanel.putClientProperty(
      s"updateLabel_$numWindow",
      (fromReload: Boolean) => updateLabel(fromReload)
    )
  }
  if contentPanel.getClientProperty(s"updateLabel_$numWindow") == null then putUpdateLabelAsClientProperty()
  else {
    contentPanel.putClientProperty(s"updateLabel_$numWindow", null)
    putUpdateLabelAsClientProperty()
  }

  def putCreateWindowAsClientProperty(): Unit = contentPanel.putClientProperty(
      "createToolWindow",
      () => createToolWindow()
    )
  if contentPanel.getClientProperty("createToolWindow") == null then putCreateWindowAsClientProperty()
  contentPanel.add(scrollPane, createGBCs(1, 0, 1, 1, GridBagConstraints.BOTH))

  project.getMessageBus
    .connect()
    .subscribe(
      VirtualFileManager.VFS_CHANGES,
      new BulkFileListener {
        override def after(events: java.util.List[? <: VFileEvent]): Unit = {
          if state.getAutoCompile then {
            state.clearOutput()
            updateLabel()
          }
        }
      }
    )

  def getContentPanel: JBPanel[?] = contentPanel

  private def updateLabel(fromReload: Boolean = false): Unit = {
    val statePath: String =
      if state != null then state.getConfPath
      else ""

    val confFile = File(statePath)

    val tabContent: Content = getToolWindowContent(numWindow)
    println(numWindow)
    println(tabContent.getTabName)
    val expectedName = genWindowName(numWindow)

    if tabContent.getTabName != expectedName then
      tabContent.setDisplayName(expectedName)

    if state.getCommand == "from" && (statePath == null || statePath.isBlank) then {
      outputLabel.setText(notConfiguredMessage)
      return
    }

    val output = state.getOutput
    if output.nonEmpty then
      outputLabel.setText(
        s"<html>${output.mkString("<br>")}</html>"
      )
    else if state.getCommand == "from" then {
      if confFile.exists() && confFile.isFile then
        runCommandForWindow(numWindow, Some(statePath))
      else
        outputLabel.setText(
          s"<html>This window's .conf file:<br>&emsp;" + statePath +
            "<br>was not found, please configure in setting</html>"
        )
    }
    else if fromReload then runCommandForWindow(numWindow)
  }

  def createToolWindow(): Unit = {
    toolWindow.createAndAddContentToTW(
      project,
      getRiddlIdeaStates.newState()
    )
  }
}
