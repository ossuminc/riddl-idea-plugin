
package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.utils.{getRiddlIdeaState, updateToolWindow}
import org.jetbrains.annotations.NotNull

class RiddlToolWindowCompileAction extends AnAction with DumbAware {
  @Override
  def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit = {
    getRiddlIdeaState.getState.clearOutput()
    updateToolWindow(true)
  }


  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.BuildLoadChanges)
  }
}
