package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{
  ActionUpdateThread,
  AnAction,
  AnActionEvent
}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.updateToolWindowRunPane
import org.jetbrains.annotations.NotNull

class RiddlToolWindowCompileAction(windowNum: Int)
    extends AnAction
    with DumbAware {

  override def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit =
    updateToolWindowRunPane(windowNum, true)

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.BuildLoadChanges)
  }

  override def getActionUpdateThread: ActionUpdateThread =
    ActionUpdateThread.BGT

}
