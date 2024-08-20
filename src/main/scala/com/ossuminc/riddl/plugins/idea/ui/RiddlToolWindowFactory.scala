package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.actionSystem.{
  ActionManager,
  ActionPlaces,
  DefaultActionGroup
}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.intellij.ui.content.ContentFactory
import com.ossuminc.riddl.plugins.idea.actions.{
  RiddlNewToolWindowAction,
  RiddlToolWindowCompileAction,
  RiddlToolWindowSettingsOpenAction
}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.utils.parsing.parseASTFromConfFile
import com.ossuminc.riddl.plugins.utils.{
  createGBCs,
  getContentManager,
  getProject,
  getRiddlIdeaStates
}
import org.jdesktop.swingx.{HorizontalLayout, VerticalLayout}

import java.awt.{GridBagConstraints, GridBagLayout}
import java.io.File
import javax.swing.{JScrollPane, ScrollPaneConstants, ScrollPaneLayout}

class RiddlToolWindowFactory extends ToolWindowFactory {
  private val settings = new RiddlIdeaSettings()

  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = {
    getRiddlIdeaStates.newState()
    toolWindow.getContentManager.addContent(
      ContentFactory
        .getInstance()
        .createContent(
          new RiddlToolWindowContent(toolWindow, project).getContentPanel,
          "riddlc",
          false
        )
    )
  }
}

class RiddlToolWindowContent(
    toolWindow: ToolWindow,
    project: Project
) {
  private val numWindow: Int = getRiddlIdeaStates.length

  private val state: RiddlIdeaSettings.State =
    getRiddlIdeaStates.getState(numWindow)

  private val notConfiguredMessage: String =
    "riddlc: project's .conf file not configured in settings"

  private val contentPanel: JBPanel[?] = new JBPanel()
  contentPanel.setLayout(VerticalLayout())
  contentPanel.setLayout(GridBagLayout())

  private val outputLabel: JBLabel = new JBLabel()
  outputLabel.setText(notConfiguredMessage)

  private val scrollPane = new JScrollPane(
    outputLabel,
    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
  )
  scrollPane.setLayout(ScrollPaneLayout())

  private val topBar: SimpleToolWindowPanel =
    new SimpleToolWindowPanel(true, false)
  topBar.setLayout(HorizontalLayout())
  private val actionGroup = new DefaultActionGroup("ToolbarRunGroup", false)
  actionGroup.add(new RiddlNewToolWindowAction)
  actionGroup.add(new RiddlToolWindowCompileAction)
  actionGroup.add(new RiddlToolWindowSettingsOpenAction)

  private val actionToolbar = ActionManager
    .getInstance()
    .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
  actionToolbar.setTargetComponent(topBar)
  topBar.setToolbar(actionToolbar.getComponent)

  contentPanel.putClientProperty(
    s"updateLabel_$numWindow",
    (fromReload: Boolean) => updateLabel(fromReload)
  )

  contentPanel.putClientProperty(
    "createToolWindow",
    () => createToolWindow()
  )

  contentPanel.add(
    topBar,
    createGBCs(0, 0, 1, 0, GridBagConstraints.HORIZONTAL)
  )
  contentPanel.add(scrollPane, createGBCs(0, 1, 0, 1, GridBagConstraints.BOTH))

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

  def updateLabel(fromReload: Boolean = false): Unit = {
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
        s"<html>File: " + statePath +
          "<br>riddlc: project's .conf file not found, please configure in setting</html>"
      )
  }

  def createToolWindow(): Unit = {
    getRiddlIdeaStates.newState()
    getContentManager.addContent(
      ContentFactory
        .getInstance()
        .createContent(
          new RiddlToolWindowContent(
            toolWindow,
            getProject
          ).getContentPanel,
          s"riddlc (${getRiddlIdeaStates.length})",
          false
        )
    )
  }
}
