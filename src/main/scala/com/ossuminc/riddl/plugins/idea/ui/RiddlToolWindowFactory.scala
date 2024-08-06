package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import com.ossuminc.riddl.plugins.utils.{
  getRiddlIdeaState,
  parseASTFromConfFile
}

import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JPanel

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

  private val windowComponent: RiddlToolWindowFactoryComponent =
    new RiddlToolWindowFactoryComponent()

  windowComponent.setLabel(notConfiguredMessage)

  windowComponent.toolWindowPanel.putClientProperty(
    "updateLabel",
    (fromReload: Boolean) => updateLabel(fromReload)
  )

  def getContentPanel: JPanel = windowComponent.toolWindowPanel

  def updateLabel(fromReload: Boolean = false): Unit = {
    val statePath: String =
      if getRiddlIdeaState != null then getRiddlIdeaState.getState.riddlConfPath
      else ""

    if statePath == null || statePath.isBlank then {
      windowComponent.setLabel(notConfiguredMessage)
      return
    }

    val confFile = File(statePath)

    if fromReload & confFile.exists() && confFile.isFile then
      parseASTFromConfFile(statePath)

    if !getRiddlIdeaState.getState.riddlOutput.isBlank then {
      windowComponent.setLabel(
        s"<html>${getRiddlIdeaState.getState.riddlOutput}</html>"
      )
    } else if confFile.exists() && confFile.isFile then {
      parseASTFromConfFile(statePath)
    } else {
      windowComponent.setLabel(
        s"<html>File: " + statePath +
          "<br>riddlc: project's .conf file not found, please configure in setting</html>"
      )
    }

  }
}
