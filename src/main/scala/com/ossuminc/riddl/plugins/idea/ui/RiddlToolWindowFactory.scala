package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ActionManager, DefaultActionGroup}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import com.ossuminc.riddl.plugins.idea.actions.RiddlToolWindowCompileAction
import com.ossuminc.riddl.plugins.utils.{
  getRiddlIdeaState,
  parseASTFromConfFile
}

import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory

class RiddlToolWindowFactory extends ToolWindowFactory {
  private def invokeLater[T](body: => T): Unit =
    ApplicationManager.getApplication.invokeLater(() => body)

  private def schedulePeriodicTask(
      delay: Long,
      unit: TimeUnit,
      parentDisposable: Disposable
  )(body: => Unit): Unit = {
    val task = AppExecutorUtil.getAppScheduledExecutorService
      .scheduleWithFixedDelay(() => body, delay, delay, unit)
    Disposer.register(
      parentDisposable,
      () => {
        task.cancel(true)
      }
    )
  }

  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = {
    val newTW = new RiddlToolWindowContent(toolWindow, project)
    val content = ContentFactory
      .getInstance()
      .createContent(
        newTW.getContentPanel,
        "riddlc",
        false
      )
    toolWindow.getContentManager.addContent(content)
  }
}

class RiddlToolWindowContent(
    toolWindow: ToolWindow,
    project: Project
) {
  private val topBar: SimpleToolWindowPanel =
    new SimpleToolWindowPanel(false, false)
  private val actionGroup = new DefaultActionGroup("ToolbarRunGroup", false)
  actionGroup.add(new RiddlToolWindowCompileAction)
  private val actionToolbar =
    ActionManager.getInstance().createActionToolbar("", actionGroup, true)
  topBar.setToolbar(actionToolbar.getComponent)

  private val contentPanel: JBPanel[Nothing] = new JBPanel()

  private val outputLabel: JBLabel = new JBLabel()

  updateLabel()

  contentPanel.putClientProperty(
    "updateLabel",
    (fromReload: Boolean) => updateLabel(fromReload)
  )
  contentPanel.setLayout(new BorderLayout(0, 20))
  contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0))
  contentPanel.add(outputLabel, BorderLayout.CENTER)
  contentPanel.add(topBar, BorderLayout.NORTH)

  def getContentPanel: JBPanel[Nothing] = contentPanel

  def updateLabel(fromReload: Boolean = false): Unit = {
    val statePath: String =
      if getRiddlIdeaState != null then getRiddlIdeaState.getState.riddlConfPath
      else ""

    if statePath == null || statePath.isBlank then {
      outputLabel.setText(
        "riddlc: project's .conf file not configured in settings"
      )
      return
    }

    val confFile = File(statePath)

    if fromReload & confFile.exists() && confFile.isFile then
      parseASTFromConfFile(statePath)

    if !getRiddlIdeaState.getState.riddlOutput.isBlank then {
      outputLabel.setText(
        s"<html>${getRiddlIdeaState.getState.riddlOutput}</html>"
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
