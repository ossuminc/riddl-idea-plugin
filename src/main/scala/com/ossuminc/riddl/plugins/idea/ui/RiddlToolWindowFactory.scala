package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import com.ossuminc.riddl.plugins.utils.{displayNotification, parseASTFromSource, riddlPluginState}
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
      if toolWindow.getProject.getBasePath != null && !toolWindow.getProject.getBasePath.isBlank
      then {
        setWindowOutput(label)
        calendarPanel.add(label)
      }
      calendarPanel
    }

    private def setWindowOutput(label: JLabel): Unit = {
      val baseDir = new File(toolWindow.getProject.getBasePath + "/" + riddlPluginState.riddlConfPath)

      def parseFromConf(): Option[Either[Messages, AST.Root]] = {
        val confFile = {
          if baseDir.exists && baseDir.isDirectory then {
            val confs = baseDir.listFiles.filter(_.getName.endsWith(".conf"))
            if confs.length != 1 then "" else confs.head.getName
          }
          else ""
        }

        if confFile.isBlank then {
          displayNotification("RIDDL: project's .conf file not found, please configure in setting")
          None
        }
        else Some(parseASTFromSource(
          URI.create("file://" + baseDir + "/" + confFile)
        ))
      }

      val textOutput =
        if baseDir.exists() then
          val astOrMsgsOpt = parseFromConf()
          if astOrMsgsOpt.isDefined then
            astOrMsgsOpt.map(astOrMessages =>
              if astOrMessages.isRight then "Compilation succeed without errors! :)"
              else astOrMessages match {
                case Left(msgs) =>
                  msgs.foreach(m =>
                    println(m.toString)
                    label.setText(m.toString))
                  msgs
                case _          => ""
              }
            )
          else ""
        else {
          displayNotification("RIDDL: project's .conf file not configured in settings")
          ""
        }
    }
  }
}
