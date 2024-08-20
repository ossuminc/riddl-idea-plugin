package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.utils.{getRiddlIdeaState, updateToolWindow}

import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable extends Configurable {
  private val numWindow = getRiddlIdeaState.getState.numToolWindows

  val component: RiddlIdeaSettingsComponent = new RiddlIdeaSettingsComponent()

  override def getDisplayName: String = "RIDDL Project Settings"

  override def createComponent(): JComponent = {
    component.getPanel
  }

  override def isModified: Boolean =
    if component.modified then {
      true
    } else false

  override def apply(): Unit = {
    if getRiddlIdeaState != null then {
      getRiddlIdeaState.getState.setConfPath(
        component.getConfFieldText
      )
      getRiddlIdeaState.getState.clearOutput()

      updateToolWindow(numWindow)
    }
  }
}
