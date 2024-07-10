package com.ossuminc.riddl.plugins.idea.configuration

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType, RunConfiguration}
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NonNls

class RiddlRunConfigurationFactory(configType: ConfigurationType) extends ConfigurationFactory(configType) {
  @NonNls
  override def getId: String = "riddl run config"

  override def createTemplateConfiguration(project: Project): RunConfiguration = new RiddlRunConfiguration(project, this, getId)
}
