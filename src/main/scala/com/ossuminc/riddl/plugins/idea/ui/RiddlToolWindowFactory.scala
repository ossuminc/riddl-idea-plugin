package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import com.ossuminc.riddl.plugins.utils.parseASTFromSource
import com.ossuminc.riddl.language.Messages.{Message, Messages}
import com.ossuminc.riddl.language.{AST, Messages}

import java.net.URI
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.{BorderFactory, JLabel, JPanel}

class RiddlToolWindowFactory extends ToolWindowFactory {

  private def invokeLater[T](body: => T): Unit = ApplicationManager.getApplication.invokeLater(() => body)

  private def schedulePeriodicTask(delay: Long, unit: TimeUnit, parentDisposable: Disposable)(body: => Unit): Unit = {
    val task = AppExecutorUtil.getAppScheduledExecutorService.scheduleWithFixedDelay(() => body, delay, delay, unit)
    Disposer.register(parentDisposable, () => {
      task.cancel(true)
    })
  }

  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = {
          val toolWindowContent = new RiddlToolWindowContent(toolWindow)
          val content = ContentFactory.getInstance().createContent(
            toolWindowContent.getContentPanel,
            "RIDDL",
            false
          )
          toolWindow.getContentManager.addContent(content)
    }

  private class RiddlToolWindowContent(toolWindow: ToolWindow) {
    private val contentPanel = new JPanel()
    private val label = new JLabel()

    contentPanel.setLayout(new BorderLayout(0, 20))
    contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0))
    contentPanel.add(createRiddlProjectOutputPanel)

    def getContentPanel: JPanel = contentPanel

    private def createRiddlProjectOutputPanel = {
      val calendarPanel = new JPanel()
      if toolWindow.getProject.getBasePath != null && toolWindow.getProject.getBasePath != ""
      then {
        setWindowOutput(label)
        calendarPanel.add(label)
      }
      calendarPanel
    }

    private def setWindowOutput(label: JLabel): Unit = {
      val astOrMessages: Either[Messages, AST.Root] = {
        val baseDir = new File(toolWindow.getProject.getBasePath + "/src/main/riddl")

        val topRiddl = {
          if baseDir.exists && baseDir.isDirectory then
            baseDir.listFiles.filter(_.getName.endsWith(".riddl")).head.getName
          else ""
        }

        if topRiddl.isBlank then Left(List(Message(0, "Top level .riddl file not found!")))
        else parseASTFromSource(
          URI.create("file://" + baseDir + "/" + topRiddl)
        )
      }

      val textOutput =
        if astOrMessages.isRight then "Compilation succeed without errors! :)"
        else
          astOrMessages match {
            case Left(msgs) =>
              msgs.foreach(m =>
                println(m.toString)
                label.setText(m.toString))
              msgs
            case _          => ""
          }
    }
  }
}
