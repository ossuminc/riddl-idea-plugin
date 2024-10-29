package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.settings.CommonOptionsUtils.{
  FiniteDurationCommonOption,
  IntegerCommonOption
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.*

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

    component.getBooleanCommonOptions.foreach(option =>
      println(option._2.name)
      windowState.setCommonOptions(
        option._2.setCommonOptionValue(windowState.getCommonOptions)(
          option._1.isSelected
        )
      )
    )

    if component.getIntegerOptionTextField.getText.forall(Character.isDigit)
    then
      windowState.setCommonOptions(
        IntegerCommonOption.setCommonOptionValue(windowState.getCommonOptions)(
          component.getIntegerOptionTextField.getText.toInt
        )
      )

    if component.getFiniteDurationOptionTextField.getText.forall(
        Character.isDigit
      )
    then
      windowState.setCommonOptions(
        FiniteDurationCommonOption.setCommonOptionValue(
          windowState.getCommonOptions
        )(
          FiniteDuration(
            component.getFiniteDurationOptionTextField.getText.toLong,
            TimeUnit.MILLISECONDS
          )
        )
      )

    val fileForPath = File(component.getConfFieldText)
    if component.getPickedCommand == "from" &&
      (fileForPath.exists() && fileForPath.isFile)
    then windowState.setConfPath(component.getConfFieldText)

    windowState.setAutoCompile(component.getAutoCompileValue)
    windowState.clearRunOutput()
    updateToolWindowPanes(numWindow, fromReload = true)
  }
}
