package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.{Configurable, ConfigurationException}
import com.ossuminc.riddl.plugins.utils.{displayNotification, riddlPluginState}

import java.io.File
import javax.swing.JComponent

class RiddlIdeaSettingsConfigurable extends Configurable {
  val component: RiddlIdeaSettingsComponent = new RiddlIdeaSettingsComponent()

  override def getDisplayName: String = "RIDDL Run Configuration"

  override def createComponent(): JComponent = component.getPanel

  override def isModified: Boolean = if component.modified then {
    component.toggleModified()
    true
  } else false

  override def apply(): Unit =
     try {
       new File(component.getConfFieldText)
     } catch {
       case _: Throwable => 
         val errorMsg = "RIDDL: .conf path invalid!"
         displayNotification(errorMsg)         
         new ConfigurationException(errorMsg)
    } finally {
       component.setConfFieldText(riddlPluginState.riddlConfPath)
       component.toggleModified()
     }
}