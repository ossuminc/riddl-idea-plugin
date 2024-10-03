package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.*

import java.io.File
import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable(numWindow: Int) extends Configurable {
  private val component: RiddlIdeaSettingsComponent = new RiddlIdeaSettingsComponent(numWindow)

  override def getDisplayName: String = "RIDDL Project Settings"

  override def createComponent(): JComponent = component.getPanel

  override def isModified: Boolean = {
    val windowState = getRiddlIdeaState(numWindow)
    if component.getAutoCompileValue != windowState.getAutoCompile then
      windowState.toggleAutoCompile()

    component.isModified
  }

  override def apply(): Unit = {
    val windowState = getRiddlIdeaState(numWindow)

    windowState.setCommand(component.getPickedCommand)

    val fileForPath = File(component.getConfFieldText)
    if component.getPickedCommand == "from" && (fileForPath.exists() && fileForPath.isFile) then
      windowState.setConfPath(component.getConfFieldText)

    if component.getAutoCompileValue != windowState.getAutoCompile then
      windowState.toggleAutoCompile()

    windowState.clearRunOutput()    
    updateToolWindowPanes(numWindow, fromReload = true)
  }
}
