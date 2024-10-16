package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.actionSystem.{
  ActionManager,
  ActionPlaces,
  ActionToolbar
}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.JBPanel
import com.ossuminc.riddl.plugins.idea.actions.{
  RiddlActionsGroup,
  RiddlNewToolWindowAction,
  RiddlToolWindowCompileAction,
  RiddlToolWindowSettingsOpenAction
}
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.*
import com.ossuminc.riddl.plugins.idea.utils.*
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.CreationUtils.*
import org.jdesktop.swingx.VerticalLayout

import java.awt.{GridBagConstraints, GridBagLayout}

class RiddlToolWindowFactory extends ToolWindowFactory {
  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit =
    toolWindow.createAndAddContentToTW(
      project,
      getRiddlIdeaStates.newState(),
      true
    )
}

class RiddlToolWindowContent(
    toolWindow: ToolWindow,
    project: Project,
    numWindow: Int
) {
  private val sideBar: SimpleToolWindowPanel =
    new SimpleToolWindowPanel(false, false)
  sideBar.setLayout(VerticalLayout())

  private val actionGroup: RiddlActionsGroup = new RiddlActionsGroup()
  actionGroup.add(new RiddlNewToolWindowAction)
  private val compileAction: RiddlToolWindowCompileAction =
    new RiddlToolWindowCompileAction(numWindow)
  actionGroup.add(compileAction)
  private val openAction: RiddlToolWindowSettingsOpenAction =
    new RiddlToolWindowSettingsOpenAction(numWindow)
  actionGroup.add(openAction)

  private val actionToolbar: ActionToolbar = ActionManager
    .getInstance()
    .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, false)
  actionToolbar.setTargetComponent(sideBar)
  sideBar.setToolbar(actionToolbar.getComponent)

  private val contentPanel: JBPanel[?] = new JBPanel()
  contentPanel.setLayout(GridBagLayout())

  contentPanel.add(sideBar, createGBCs(0, 0, 0, 0, GridBagConstraints.VERTICAL))

  createRunConsole(toolWindow, contentPanel, numWindow, project)

  def getContentPanel: JBPanel[?] = contentPanel
}
