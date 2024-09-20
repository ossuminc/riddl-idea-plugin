package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{
  ActionUpdateThread,
  AnAction,
  AnActionEvent
}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.openToolWindowSettings
import org.jetbrains.annotations.NotNull

class RiddlToolWindowSettingsOpenAction extends AnAction with DumbAware {
  private var windowNum: Int = 1
  def setWindowNum(num: Int): Unit = windowNum = num

  override def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit =
    openToolWindowSettings(windowNum)

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.InlayGear)
  }

  override def getActionUpdateThread: ActionUpdateThread =
    ActionUpdateThread.BGT
}
