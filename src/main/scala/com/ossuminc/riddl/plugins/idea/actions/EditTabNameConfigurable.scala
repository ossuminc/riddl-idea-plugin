/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.actions

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.{JBPanel, JBTextField}
import com.ossuminc.riddl.plugins.idea.utils.ToolWindowUtils.*

import java.awt.event.ActionListener
import javax.swing.JComponent

class EditTabNameConfigurable(numWindow: Int) extends Configurable {
  private val editTabNamePanel: JBPanel[Nothing] = new JBPanel()
  private val newNameField: JBTextField = new JBTextField("Enter new tab name:")
  private var isComponentModified: Boolean = false

  newNameField.addActionListener(_ => isComponentModified = true)

  editTabNamePanel.add(newNameField)

  override def getDisplayName: String = "Edit RIDDL Tab Name"

  override def createComponent(): JComponent = editTabNamePanel

  override def isModified: Boolean = if isComponentModified then true else false

  override def apply(): Unit = {
    getToolWindowContent(numWindow).setDisplayName(newNameField.getText)
  }
}
