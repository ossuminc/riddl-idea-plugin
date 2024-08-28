package com.ossuminc.riddl.plugins.idea.actions;

import com.intellij.openapi.actionSystem.{
  AnAction,
  AnActionEvent,
  PlatformDataKeys
}
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindow

import javax.swing.JOptionPane;

class EditTabNameAction extends AnAction with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val toolWindow: ToolWindow = e.getData(PlatformDataKeys.TOOL_WINDOW);
    if toolWindow != null then {
      val newName = JOptionPane.showInputDialog("Enter new tab name:");
      if newName != null && newName.trim.nonEmpty then {
        toolWindow.getContentManager.getSelectedContent.setDisplayName(newName);
      }
    }
  }
}
