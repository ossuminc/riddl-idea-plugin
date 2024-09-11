package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{
  ActionUpdateThread,
  AnAction,
  AnActionEvent
}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.utils.ToolWindowUtils.updateToolWindow
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.getRiddlIdeaState
import org.jetbrains.annotations.NotNull

class RiddlToolWindowCompileAction extends AnAction with DumbAware {
  private var windowNum: Int = -1
  def setWindowNum(num: Int): Unit = windowNum = num

  override def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit = {
    getRiddlIdeaState(windowNum).clearOutput()
    updateToolWindow(windowNum, true)
  }

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.BuildLoadChanges)
  }

  override def getActionUpdateThread: ActionUpdateThread =
    ActionUpdateThread.BGT

}
