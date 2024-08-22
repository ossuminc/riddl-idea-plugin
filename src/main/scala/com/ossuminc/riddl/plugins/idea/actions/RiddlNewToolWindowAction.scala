package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.createNewToolWindow
import org.jetbrains.annotations.NotNull

class RiddlNewToolWindowAction extends AnAction with DumbAware {
  override def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit = {
    createNewToolWindow()
  }

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.AddMulticaret)
  }

  override def getActionUpdateThread: ActionUpdateThread =
    ActionUpdateThread.BGT
}
