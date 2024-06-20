package com.ossuminc.riddl.plugins.idea.configuration

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle

import javax.swing.Icon

class RiddlRunConfigurationType extends ConfigurationType with DumbAware {
  val confFactory = new RiddlRunConfigurationFactory(this)

  override def getDisplayName: String = RiddlIdeaPluginBundle.message("riddl.plugins.idea.displayName")

  override def getConfigurationTypeDescription: String = RiddlIdeaPluginBundle.message("riddl.plugins.idea.description")

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getId: String = "RiddlRunConfiguration"
}
