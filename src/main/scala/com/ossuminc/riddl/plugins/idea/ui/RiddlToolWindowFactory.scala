package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.{JBLabel, JBPanel}
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.messages.Topic
import com.ossuminc.riddl.plugins.utils.{
  fullPathToConf,
  getRiddlIdeaState,
  parseASTFromSource
}
import com.ossuminc.riddl.language.Messages.Message
import com.ossuminc.riddl.language.Messages
import com.ossuminc.riddl.plugins.idea.settings.ChangeActionNotifier

import java.net.URI
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.{BorderFactory, JPanel}

class RiddlToolWindowFactory extends ToolWindowFactory {
  private var toolWindowContent: Option[RiddlToolWindowContent] = None

  //  @Topic.ProjectLevel
  //  private trait UPDATE_TOOL_WINDOW_TOPIC
//  
  //  private object UPDATE_TOOL_WINDOW_TOPIC {
  //    def createTopic: Topic[ChangeActionNotifier] =
  //      Topic.create("updateRiddlToolWindow", classOf[ChangeActionNotifier])
//  
  //    def updateWindow(): Unit = update()
  //  }

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
        "RIDDL",
        false
      )
    toolWindow.getContentManager.addContent(content)
    toolWindowContent = Some(newTW)

    //Topic.create(
    //  "RIDDL_TOOL_WINDOW",
    //  classOf[ChangeActionNotifier]
    //)
  }

  def update(): Unit = {
    println(toolWindowContent)
    if toolWindowContent.isDefined then toolWindowContent.get.updateLabel()
  }

  private class RiddlToolWindowContent(
      toolWindow: ToolWindow,
      project: Project
  ) {
    private val contentPanel = new JBPanel()
    private val label = new JBLabel()

    updateLabel()

    contentPanel.setLayout(new BorderLayout(0, 20))
    contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0))
    contentPanel.add(label)

    def getContentPanel: JPanel = contentPanel

    def updateLabel(): Unit = {
      val statePath: String = getRiddlIdeaState.riddlConfPath

      if statePath.isBlank then {
        label.setText(
          "RIDDL: project's .conf file not configured in settings"
        )
        return
      }

      val confPath = fullPathToConf(
        toolWindow.getProject.getBasePath,
        statePath
      )

      val confFile = File(confPath)

      if !confFile.exists() then
        label.setText(
          "RIDDL: project's .conf file not found, please configure in setting"
        )
      else
        parseASTFromSource(URI(confPath)) match {
          case Left(msgs: List[Messages.Message]) =>
            msgs.foreach(m =>
              println(m.toString)
              label.setText(m.toString)
            )
          case _ => label.setText("Compilation succeed without errors! :)")
        }
        ()
    }
  }
}
