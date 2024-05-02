// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.ContentFactory

import java.awt.BorderLayout
import javax.swing.{BorderFactory, JPanel}

class RiddlToolWindowFactory extends ToolWindowFactory {

  override def createToolWindowContent(
      project: Project,
      toolWindow: ToolWindow
  ): Unit = {
    val toolWindowContent = new RiddlToolWindowContent(toolWindow)
    val content = ContentFactory.getInstance.createContent(
      toolWindowContent.getContentPanel,
      "",
      false
    )
    toolWindow.getContentManager.addContent(content)
  }

  class RiddlToolWindowContent(toolWindow: ToolWindow) {
    private val contentPanel = new JPanel()
    contentPanel.setLayout(new BorderLayout(0, 20))
    contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0))

    def getContentPanel: JPanel = contentPanel
  }
}
