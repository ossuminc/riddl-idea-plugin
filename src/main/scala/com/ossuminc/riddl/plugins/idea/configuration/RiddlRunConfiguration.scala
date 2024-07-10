package com.ossuminc.riddl.plugins.idea.configuration

import com.intellij.execution.{ExecutionResult, Executor, OutputListener}
import com.intellij.execution.configurations.{CommandLineState, ConfigurationFactory, GeneralCommandLine, ModuleBasedConfiguration, RunConfiguration, RunConfigurationModule, RunProfileState}
import com.intellij.execution.process.{CapturingProcessHandler, OSProcessHandler, ProcessAdapter, ProcessEvent, ProcessHandler}
import com.intellij.execution.runners.{ExecutionEnvironment, ProgramRunner}
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.util.execution.ParametersListUtil
import com.ossuminc.riddl.language.AST
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.plugins.idea.{displayNotification, parseASTFromSource, riddlPluginState}
import org.jdom.Element

import java.io.File
import java.net.URI
import java.util

class RiddlRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String)
  extends ModuleBasedConfiguration[
    RunConfigurationModule,Element
  ](name, new RunConfigurationModule(project), configurationFactory) {

  private var confPath: String = ""

  def apply(params: RiddlRunConfigurationForm): Unit = {
    confPath = params.getConfPath
  }

  override def getValidModules: util.Collection[Module] = new java.util.ArrayList

  override def getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = new RiddlCommandLineState(environment)

  override def getConfigurationEditor: SettingsEditor[? <: RunConfiguration] = new RiddlRunConfigurationEditor(project, this)

  def getWorkingDir = new String(confPath.toCharArray)

  private class RiddlCommandLineState(
    environment: ExecutionEnvironment
  ) extends CommandLineState(environment) {
    override def startProcess(): ProcessHandler = {
      val cmdProcess = new GeneralCommandLine()
      cmdProcess.setExePath("riddlc")
      cmdProcess.addParameter("from")
      cmdProcess.addParameter(confPath)
      cmdProcess.addParameter("validate")
      val handler = new OSProcessHandler(cmdProcess)
      handler.startNotify()
      handler
    }
  }
}
