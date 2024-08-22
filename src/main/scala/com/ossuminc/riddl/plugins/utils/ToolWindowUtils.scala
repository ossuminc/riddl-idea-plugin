package com.ossuminc.riddl.plugins.utils

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.ui.content.{
  Content,
  ContentFactory,
  ContentManager,
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

      listenForContentRemoval(project, windowName, windowNumber)

      getContentManager.addContent(content)
    }

    private def listenForContentRemoval(
        project: Project,
        windowDisplayName: String,
        windowNum: Int
    ): Unit = ToolWindowManager
      .getInstance(project)
      .getToolWindow("riddl")
      .getContentManager
      .addContentManagerListener(
        new ContentManagerListener() {
          private val windowName: String = windowDisplayName
          private val windowNumber: Int = windowNum

          override def contentRemoved(event: ContentManagerEvent): Unit = {
            val content = event.getContent
            if event.getContent.getDisplayName == windowName then {
              content.getComponent.putClientProperty(
                s"updateLabel_$windowNumber",
                null
              )
              ToolWindowManager
                .getInstance(project)
                .getToolWindow("riddl")
                .getContentManager
                .removeContentManagerListener(this)
              getRiddlIdeaStates.removeState(windowNumber)
            }
          }
        }
      )
  }

  def getContentManager: ContentManager = ToolWindowManager
    .getInstance(
      getProject
    )
    .getToolWindow("riddl")
    .getContentManager

  private def getToolWindowContent(numWindow: Int): Content =
    getContentManager.getContent(
      getRiddlIdeaStates.getStates.keys.zipWithIndex
        .find((num, index) => num == numWindow)
        .map(_._2)
        .getOrElse(-1)
    )

  def updateToolWindow(numWindow: Int, fromReload: Boolean = false): Unit = {
    getToolWindowContent(numWindow).getComponent
      .getClientProperty(s"updateLabel_$numWindow")
      .asInstanceOf[(fromReload: Boolean) => Unit](fromReload)
  }

  def createNewToolWindow(): Unit =
    getToolWindowContent(0).getComponent
      .getClientProperty("createToolWindow")
      .asInstanceOf[() => Unit]()

  def openToolWindowSettings(numWindow: Int): Unit =
    ShowSettingsUtil.getInstance
      .editConfigurable(
        getProject,
        new RiddlIdeaSettingsConfigurable(numWindow)
      )
}
