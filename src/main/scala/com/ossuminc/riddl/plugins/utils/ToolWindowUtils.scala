package com.ossuminc.riddl.plugins.utils

import com.intellij.openapi.ide.CopyPasteManager.ContentChangedListener
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.ui.content.{
  Content,
  ContentFactory,
  ContentManagerEvent,
  ContentManagerListener
}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettingsConfigurable
import com.ossuminc.riddl.plugins.idea.ui.RiddlToolWindowContent

object ToolWindowUtils {
  import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*

  implicit class ToolWindowExt(toolWindow: ToolWindow) {
    def createAndAddContentToTW(
        project: Project,
        windowNumber: Int,
        isLockable: Boolean = false
    ): Unit = {
      val windowName: String =
        if windowNumber == 0 then "riddlc"
        else s"riddlc ($windowNumber)"

      val content = ContentFactory
        .getInstance()
        .createContent(
          new RiddlToolWindowContent(
            toolWindow,
            project,
            windowNumber
          ).getContentPanel,
          windowName,
          isLockable
        )
      content.setCloseable(!isLockable)

      ToolWindowManager
        .getInstance(project)
        .getToolWindow("riddl")
        .getContentManager
        .addContentManagerListener(
          new ContentManagerListener() {
            private val windowDisplayName = windowName
            private val windowNum = windowNumber

            override def contentRemoved(event: ContentManagerEvent): Unit = {
              if event.getContent.getDisplayName == windowDisplayName then {
                getRiddlIdeaStates.removeState(windowNumber)
                ToolWindowManager
                  .getInstance(project)
                  .getToolWindow("riddl")
                  .getContentManager
                  .removeContentManagerListener(this)
                println(getRiddlIdeaStates.getStates.keys)
              }
            }
          }
        )

      getContentManager.addContent(content)
    }
  }

  private def getToolWindowContent(numWindow: Int): Content =
    getContentManager.getContent(numWindow - 1)

  def updateToolWindow(numWindow: Int, fromReload: Boolean = false): Unit = {
    getToolWindowContent(numWindow).getComponent
      .getClientProperty(s"updateLabel_$numWindow")
      .asInstanceOf[(fromReload: Boolean) => Unit](fromReload)
  }

  def createNewToolWindow(): Unit =
    getToolWindowContent(1).getComponent
      .getClientProperty("createToolWindow")
      .asInstanceOf[() => Unit]()

  def openToolWindowSettings(numWindow: Int): Unit =
    ShowSettingsUtil.getInstance
      .editConfigurable(
        getProject,
        new RiddlIdeaSettingsConfigurable(numWindow)
      )
}
