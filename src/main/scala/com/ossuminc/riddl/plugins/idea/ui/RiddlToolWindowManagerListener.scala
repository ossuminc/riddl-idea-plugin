package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaState

class RiddlToolWindowManagerListener extends ToolWindowManagerListener {
  override def toolWindowUnregistered(
      id: String,
      toolWindow: ToolWindow
  ): Unit = {
    super.toolWindowUnregistered(id, toolWindow)
    val windowNum: Int = if id.exists(_.isDigit) then
      val regex = """(\d+)""".r
      val regexResult = regex.findFirstIn(id)
      if regexResult.isDefined then regexResult.get.toInt
      else 1
    else 1

    getRiddlIdeaState(windowNum).disconnectVFSListener
  }
}
