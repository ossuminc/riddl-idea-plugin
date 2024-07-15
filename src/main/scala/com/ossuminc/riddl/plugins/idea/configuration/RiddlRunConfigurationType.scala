package com.ossuminc.riddl.plugins.idea.configuration

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle

import javax.swing.Icon
import com.ossuminc.riddl.plugins.idea.Icons.RIDDL_ICON

class RiddlRunConfigurationType extends ConfigurationType with DumbAware {
  private val confFactory = new RiddlRunConfigurationFactory(this)

  override def getDisplayName: String = RiddlIdeaPluginBundle.message("riddl.plugins.idea.displayName")

  override def getConfigurationTypeDescription: String = RiddlIdeaPluginBundle.message("riddl.plugins.idea.description")

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array(confFactory)

  override def getIcon: Icon = RIDDL_ICON
  
  override def getId: String = "riddlRunConfiguration"
}
