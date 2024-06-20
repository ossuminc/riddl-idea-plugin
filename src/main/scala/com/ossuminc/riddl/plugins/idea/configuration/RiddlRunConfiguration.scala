package com.ossuminc.riddl.plugins.idea.configuration

import com.intellij.execution.{ExecutionResult, Executor, OutputListener}
import com.intellij.execution.configurations.{CommandLineState, ConfigurationFactory, ModuleBasedConfiguration, RunConfiguration, RunConfigurationModule, RunProfileState}
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.{ExecutionEnvironment, ProgramRunner}
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element

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

  override def getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = new RiddlCommandLineState("", this, environment)

  override def getConfigurationEditor: SettingsEditor[? <: RunConfiguration] = new RiddlRunConfigurationEditor(project, this)

  def getWorkingDir = new String(confPath.toCharArray)
}

class RiddlCommandLineState (
  val processedCommands: String,
  val configuration: RiddlRunConfiguration,
  environment: ExecutionEnvironment,
  private var listener: Option[String => Unit] = None
) extends CommandLineState(environment){
  def getListener: Option[String => Unit] = listener

  override def execute(executor: Executor, runner: ProgramRunner[?]): ExecutionResult = {
    val r = super.execute(executor, runner)
    listener.foreach(_ => Option(r.getProcessHandler).foreach(_.addProcessListener(new OutputListener() {
      override def onTextAvailable(event: ProcessEvent, outputType: Key[?]): Unit = super.onTextAvailable(event, outputType)
    })))
    r
  }
}
