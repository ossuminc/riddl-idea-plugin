/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.actions;

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.options.Configurable
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getProject

class EditTabNameAction(numWindow: Int = -1) extends AnAction with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit = {
    ShowSettingsUtil.getInstance
      .editConfigurable(
        getProject,
        new EditTabNameConfigurable(numWindow).asInstanceOf[Configurable]
      )
  }
}
