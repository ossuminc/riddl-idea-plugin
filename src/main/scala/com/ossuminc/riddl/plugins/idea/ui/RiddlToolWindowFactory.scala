// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.ContentFactory
import com.ossuminc.riddl.plugins.utils.parseASTFromSource
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.{AST, Messages}

import java.net.URI
import java.awt.BorderLayout
import javax.swing.{BorderFactory, JLabel, JPanel}

class RiddlToolWindowFactory extends ToolWindowFactory {

  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = {
    val toolWindowContent = new RiddlToolWindowContent(toolWindow)
    val content = ContentFactory.getInstance.createContent(
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
      val astOrMessages: Either[Messages, AST.Root] = parseASTFromSource(
        URI.create(toolWindow.getProject.getBasePath)
      )

      val textOutput =
        if astOrMessages.isRight then "Compilation succeed without errors! :)"
        else
          astOrMessages match {
            case Left(msgs) => msgs.mkString("\n")
            case _          => ""
          }

      System.out.println(
        toolWindow.getProject.getBasePath
      )

      System.out.println(
        textOutput
      )
      label.setText(
        textOutput
      )
    }
  }
}
