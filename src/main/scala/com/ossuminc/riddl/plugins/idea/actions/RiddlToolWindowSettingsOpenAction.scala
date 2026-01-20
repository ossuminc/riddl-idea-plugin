/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{
  ActionUpdateThread,
  AnAction,
  AnActionEvent
}
import com.intellij.openapi.project.DumbAware
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.openToolWindowSettings
import org.jetbrains.annotations.NotNull

class RiddlToolWindowSettingsOpenAction(windowNum: Int)
    extends AnAction
    with DumbAware {

  override def actionPerformed(@NotNull anActionEvent: AnActionEvent): Unit =
    openToolWindowSettings(windowNum)

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    e.getPresentation.setIcon(AllIcons.Actions.InlayGear)
  }

  override def getActionUpdateThread: ActionUpdateThread =
    ActionUpdateThread.BGT
}
