package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.*

import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable(numWindow: Int) extends Configurable {
  private val component: RiddlIdeaSettingsComponent =
    new RiddlIdeaSettingsComponent(
      numWindow
    )

  override def getDisplayName: String = "RIDDL Project Settings"

  override def createComponent(): JComponent = {
    component.getPanel
  }

  override def isModified: Boolean =
    if component.modified then {
      true
    } else false

  override def apply(): Unit = {
    val windowState = getRiddlIdeaState(numWindow)
    windowState.setConfPath(
      component.getConfFieldText
    )
    windowState.clearOutput()
    
    println(numWindow)
    updateToolWindow(numWindow)
  }
}
