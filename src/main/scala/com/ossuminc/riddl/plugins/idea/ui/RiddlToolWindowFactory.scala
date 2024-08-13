package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.actionSystem.{
  ActionManager,
  ActionPlaces,
  DefaultActionGroup
}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.intellij.ui.content.ContentFactory
import com.ossuminc.riddl.plugins.idea.actions.{
  RiddlToolWindowCompileAction,
  RiddlToolWindowSettingsOpenAction
}
import com.ossuminc.riddl.plugins.utils.{
  formatParsedResults,
  getRiddlIdeaState,
  parseASTFromConfFile
}
import org.jdesktop.swingx.{HorizontalLayout, VerticalLayout}

import java.awt.{GridBagConstraints, GridBagLayout}
import java.io.File
import javax.swing.{JPanel, JScrollPane, ScrollPaneConstants, ScrollPaneLayout}

class RiddlToolWindowFactory extends ToolWindowFactory {
  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = toolWindow.getContentManager.addContent(
    ContentFactory
      .getInstance()
      .createContent(
        new RiddlToolWindowContent(toolWindow, project).getContentPanel,
        "riddlc",
        false
      )
  )
}

class RiddlToolWindowContent(
    toolWindow: ToolWindow,
    project: Project
) {
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
  actionGroup.add(new RiddlToolWindowCompileAction)
  actionGroup.add(new RiddlToolWindowSettingsOpenAction)

  private val actionToolbar = ActionManager
    .getInstance()
    .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
  actionToolbar.setTargetComponent(topBar)
  topBar.setToolbar(actionToolbar.getComponent)

  contentPanel.putClientProperty(
    "updateLabel",
    (fromReload: Boolean) => updateLabel(fromReload)
  )

  private val topBarGBCs: GridBagConstraints = new GridBagConstraints();
  topBarGBCs.gridx = 0
  topBarGBCs.gridy = 0
  topBarGBCs.weightx = 1
  topBarGBCs.weighty = 0
  topBarGBCs.fill = GridBagConstraints.HORIZONTAL
  contentPanel.add(topBar, topBarGBCs)

  private val panelGBCs: GridBagConstraints = new GridBagConstraints();
  panelGBCs.gridx = 0
  panelGBCs.gridy = 1
  panelGBCs.weightx = 0
  panelGBCs.weighty = 1
  panelGBCs.fill = GridBagConstraints.BOTH
  contentPanel.add(scrollPane, panelGBCs)

  project.getMessageBus
    .connect()
    .subscribe(
      FileDocumentManagerListener.TOPIC,
      new FileDocumentManagerListener() {
        override def beforeDocumentSaving(doc: Document): Unit = {
          updateLabel(true)
        }
      }
    )

  def getContentPanel: JBPanel[Nothing] = contentPanel

  def updateLabel(fromReload: Boolean = false): Unit = {
    val statePath: String =
      if getRiddlIdeaState != null then getRiddlIdeaState.getState.riddlConfPath
      else ""

    if statePath == null || statePath.isBlank then {
      outputLabel.setText(notConfiguredMessage)
      return
    }

    val confFile = File(statePath)

    if fromReload & confFile.exists() && confFile.isFile then
      parseASTFromConfFile(statePath)

    if getRiddlIdeaState.getState.riddlOutput.nonEmpty then {
      outputLabel.setText(
        s"<html>${formatParsedResults(getRiddlIdeaState.getState.riddlOutput)}</html>"
      )
    } else if confFile.exists() && confFile.isFile then {
      parseASTFromConfFile(statePath)
    } else {
      outputLabel.setText(
        s"<html>File: " + statePath +
          "<br>riddlc: project's .conf file not found, please configure in setting</html>"
      )
    }

  }
}
