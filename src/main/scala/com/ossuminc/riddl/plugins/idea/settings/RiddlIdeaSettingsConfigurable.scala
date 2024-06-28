package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaTopics.UpdateToolWindow.UpdateToolWindowListener
import com.ossuminc.riddl.plugins.utils.{getProject, getRiddlIdeaState}

import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable extends Configurable {
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
    val toolListener = new UpdateToolWindowListener

    println("applying save")
    if getRiddlIdeaState != null then
      getRiddlIdeaState.setConfPath(component.getConfFieldText)
    println("applying message")
    getProject.getMessageBus
      .syncPublisher(
        toolListener.listenerTopic.TOPIC
      )
      .settingsChanged()
  }
}
