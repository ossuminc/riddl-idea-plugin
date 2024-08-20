package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.utils.{
  getRiddlIdeaState,
  getRiddlIdeaStates,
  updateToolWindow
}

import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable extends Configurable {
  private val numWindow = getRiddlIdeaStates.length

  val component: RiddlIdeaSettingsComponent = new RiddlIdeaSettingsComponent(
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
    val window = getRiddlIdeaState(numWindow)
    window.setConfPath(
      component.getConfFieldText
    )
    window.clearOutput()

    updateToolWindow(numWindow)
  }
}
