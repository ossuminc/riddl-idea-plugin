package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder

import java.io.File
import javax.swing.JPanel

class RiddlIdeaSettingsComponent {
  private val confFilePath: Option[File] = None
  private val confFileText = ""
  private val confFileTextField = new JBTextField()
  var modified = false

  private def riddlMainPanel = FormBuilder.createFormBuilder.addLabeledComponent(
      "Enter conf file path relative to project base:", confFileTextField, 1, false
    )
    .addComponentFillVertically(new JPanel(), 0)
    .getPanel

  def getPanel: JPanel = riddlMainPanel

  def getPreferredFocusedComponent: String = confFileText

  def getConfFieldText: String = confFileTextField.getText

  def setConfFieldText(newText: String): Unit = {
    confFileTextField.setText(newText)
    modified = true
  }

  def toggleModified(): Unit = modified = !modified
}