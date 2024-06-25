package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.settings
import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState

import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable extends Configurable {
  val component: RiddlIdeaSettingsComponent = new RiddlIdeaSettingsComponent

  override def getDisplayName: String = "RIDDL Project Settings"

  override def createComponent(): JComponent = {
    component.getPanel
  }

  override def isModified: Boolean = if component.modified then {
    true
  } else false

  override def apply(): Unit = {
    getRiddlIdeaState.setConfPath(component.getConfFieldText)
  }
}
