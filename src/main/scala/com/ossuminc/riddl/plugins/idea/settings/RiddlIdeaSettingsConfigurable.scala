package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.*

import java.io.File
import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable(numWindow: Int) extends Configurable {
  private val component: RiddlIdeaSettingsComponent =
    new RiddlIdeaSettingsComponent(numWindow)

  override def getDisplayName: String = "RIDDL Project Settings"

  override def createComponent(): JComponent = component.getPanel

  override def isModified: Boolean = component.isModified

  override def apply(): Unit = {
    val windowState = getRiddlIdeaState(numWindow)

    windowState.setCommand(component.getPickedCommand)

    val fileForPath = File(component.getConfFieldText)
    if component.getPickedCommand == "from" &&
      (fileForPath.exists() && fileForPath.isFile)
    then windowState.setConfPath(component.getConfFieldText)

    windowState.setAutoCompile(component.getAutoCompileValue)

    windowState.clearRunOutput()
    updateToolWindowPanes(numWindow, fromReload = true)
  }
}
