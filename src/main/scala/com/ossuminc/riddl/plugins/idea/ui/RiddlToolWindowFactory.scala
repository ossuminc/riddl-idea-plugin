package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.actionSystem.{
  ActionManager,
  ActionPlaces,
  ActionToolbar,
  DefaultActionGroup
}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.ossuminc.riddl.plugins.idea.actions.{
  RiddlNewToolWindowAction,
  RiddlToolWindowCompileAction,
  RiddlToolWindowSettingsOpenAction
}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.utils.ParsingUtils.parseASTFromConfFile
import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.*
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.utils.CreationUtils.*
import org.jdesktop.swingx.VerticalLayout

import java.awt.{GridBagConstraints, GridBagLayout}
import java.io.File
import javax.swing.border.EmptyBorder
import javax.swing.plaf.metal.MetalBorders.ScrollPaneBorder
import javax.swing.{
  JScrollPane,
  ScrollPaneConstants,
  ScrollPaneLayout,
  SwingConstants
}

class RiddlToolWindowFactory extends ToolWindowFactory {
  private val settings = new RiddlIdeaSettings()

  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = {
    getRiddlIdeaStates.newState()
    toolWindow.createAndAddContentToTW(project, 0, true)
  }
}

class RiddlToolWindowContent(
    toolWindow: ToolWindow,
    project: Project,
    numWindow: Int
) {

  private val state: RiddlIdeaSettings.State =
    getRiddlIdeaStates.getState(numWindow)

  private val notConfiguredMessage: String =
    "project's .conf file not configured in settings"

  private val contentPanel: JBPanel[?] = new JBPanel()
  contentPanel.setLayout(GridBagLayout())

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

  private val sideBar: SimpleToolWindowPanel =
    new SimpleToolWindowPanel(false, false)
  sideBar.setLayout(VerticalLayout())

  private val actionGroup: DefaultActionGroup =
    new DefaultActionGroup("ToolWindowToolbarRunGroup", false)
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

  contentPanel.putClientProperty(
    s"updateLabel_$numWindow",
    (fromReload: Boolean) => updateLabel(fromReload)
  )

  contentPanel.putClientProperty(
    "createToolWindow",
    () => createToolWindow()
  )

  contentPanel.add(
    sideBar,
    createGBCs(0, 0, 0, 0, GridBagConstraints.VERTICAL)
  )
  contentPanel.add(scrollPane, createGBCs(1, 0, 1, 1, GridBagConstraints.BOTH))

  project.getMessageBus
    .connect()
    .subscribe(
      VirtualFileManager.VFS_CHANGES,
      new BulkFileListener {
        override def after(events: java.util.List[? <: VFileEvent]): Unit = {
          if state.autoCompileOnSave then {
            state.clearOutput()
            updateLabel()
          }
        }
      }
    )

  def getContentPanel: JBPanel[?] = contentPanel

  private def updateLabel(fromReload: Boolean = false): Unit = {
    val statePath: String =
      if state != null then state.riddlConfPath
      else ""

    if statePath == null || statePath.isBlank then {
      outputLabel.setText(notConfiguredMessage)
      return
    }

    val confFile = File(statePath)

    val output = state.riddlOutput
    if output.nonEmpty then
      outputLabel.setText(
        s"<html>${output.mkString("<br>")}</html>"
      )
    else if fromReload || (confFile.exists() && confFile.isFile) then
      parseASTFromConfFile(numWindow, statePath)
    else
      outputLabel.setText(
        s"<html>This window's .conf file:<br>&emsp;" + statePath +
          "<br>was not found, please configure in setting</html>"
      )
  }

  def createToolWindow(): Unit = {
    toolWindow.createAndAddContentToTW(
      project,
      getRiddlIdeaStates.newState()
    )
  }
}
