package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import com.ossuminc.riddl.language.Messages.Message
import com.ossuminc.riddl.language.Messages
import com.ossuminc.riddl.plugins.utils.{
  getRiddlIdeaState,
  parseASTFromSource
}

import java.net.URI
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

  private val contentPanel: JBPanel[Nothing] = new JBPanel()
  private val label: JBLabel = new JBLabel()

  updateLabel()

  contentPanel.putClientProperty("updateLabel", () => updateLabel())
  contentPanel.setLayout(new BorderLayout(0, 20))
  contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0))
  contentPanel.add(label)

  def getContentPanel: JBPanel[Nothing] = contentPanel

  def updateLabel(): Unit = {
    val statePath: String =
      if getRiddlIdeaState != null then getRiddlIdeaState.getState.riddlConfPath
      else ""

    if statePath == null || statePath.isBlank then {
      label.setText(
        "riddlc: project's .conf file not configured in settings"
      )
      return
    }

    val confFile = File(statePath)
    if confFile.exists() then {
      println(statePath)
      parseASTFromSource(URI("file://" + statePath)) match {
        case Left(msgs: List[Messages.Message]) =>
          msgs.foreach(m => println(m.toString))
          label.setText(msgs.mkString("\n"))
        case _ =>
          label.setText("Compilation succeed without errors! :)")
      }
    } else {
      label.setText(
        "File: " + statePath +
          "\nriddlc: project's .conf file not found, please configure in setting"
      )
    }
  }
}
