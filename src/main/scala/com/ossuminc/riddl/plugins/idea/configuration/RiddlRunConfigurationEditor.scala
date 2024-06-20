package com.ossuminc.riddl.plugins.idea.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettingsComponent

import javax.swing.JComponent

class RiddlRunConfigurationEditor(project: Project, configuration: RiddlRunConfiguration)
  extends SettingsEditor[RiddlRunConfiguration] {
  val form = new RiddlRunConfigurationForm(project, configuration)

  override def resetEditorFrom(configuration: RiddlRunConfiguration): Unit = form(configuration)

  override def applyEditorTo(configuration: RiddlRunConfiguration): Unit = ()

  override def createEditor: JComponent = form.getConfFilePanel
}
