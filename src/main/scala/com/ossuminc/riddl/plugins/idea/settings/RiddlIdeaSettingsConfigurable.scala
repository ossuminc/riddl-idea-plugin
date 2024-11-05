package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.settings.CommonOptionsUtils.{
  FiniteDurationCommonOption,
  IntegerCommonOption
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.*
import org.codehaus.groovy.control.ConfigurationException

import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import scala.concurrent.duration.FiniteDuration

class RiddlIdeaSettingsConfigurable(numWindow: Int) extends Configurable {
  private val component: RiddlIdeaSettingsComponent =
    new RiddlIdeaSettingsComponent(numWindow)

  override def getDisplayName: String = "RIDDL Project Settings"

  override def createComponent(): JComponent = component.getPanel

  override def isModified: Boolean = component.isModified

  override def apply(): Unit = {
    val windowState = getRiddlIdeaState(numWindow)

    windowState.setCommand(component.getPickedCommand)

    windowState.setTopLevelPath(component.getTopLevelFieldText)

    val fileForPath = File(component.getConfFieldText)
    if component.getPickedCommand == "from" &&
      (fileForPath.exists() && fileForPath.isFile)
    then windowState.setConfPath(component.getConfFieldText)

    component.getBooleanCommonOptions.foreach(option =>
      windowState.setCommonOptions(
        option._2.setCommonOptionValue(windowState.getCommonOptions)(
          option._1.isSelected
        )
      )
    )

    if component.getIntegerOptionTextField.getText.forall(Character.isDigit)
    then
      if component.getIntegerOptionTextField.getText.toInt < Runtime.getRuntime.availableProcessors * 2
      then
        windowState.setCommonOptions(
          IntegerCommonOption.setCommonOptionValue(
            windowState.getCommonOptions
          )(
            component.getIntegerOptionTextField.getText.toInt
          )
        )
      else
        ConfigurationException(
          s"max-parallel-parsing must be less than ${Runtime.getRuntime.availableProcessors * 2}"
        )
    else ConfigurationException("max-parallel-parsing must be an integer")

    if component.getFiniteDurationOptionTextField.getText.forall(
        Character.isDigit
      )
    then
      val finiteDuration = FiniteDuration(
        component.getFiniteDurationOptionTextField.getText.toLong,
        TimeUnit.MILLISECONDS
      )
      if finiteDuration.toMillis < 60000 then
        windowState.setCommonOptions(
          FiniteDurationCommonOption.setCommonOptionValue(
            windowState.getCommonOptions
          )(finiteDuration)
        )
      else ConfigurationException("max-include-wait must be less than 1 minute")
    else ConfigurationException("max-include-wait must be an integer")

    windowState.setAutoCompile(component.getAutoCompileValue)
    windowState.clearRunOutput()
    runCommandForEditor(windowState.getWindowNum)
    updateToolWindowPanes(numWindow, fromReload = true)
  }
}
