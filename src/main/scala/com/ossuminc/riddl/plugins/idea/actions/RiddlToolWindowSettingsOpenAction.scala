package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{
  ActionUpdateThread,
  AnAction,
  AnActionEvent
}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.utils.openToolWindowSettings
import org.jetbrains.annotations.NotNull

class RiddlToolWindowSettingsOpenAction extends AnAction with DumbAware {
  @Override
  def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit = {
    openToolWindowSettings()
  }

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.InlayGear)
  }

  override def getActionUpdateThread: ActionUpdateThread =
    ActionUpdateThread.BGT
}
