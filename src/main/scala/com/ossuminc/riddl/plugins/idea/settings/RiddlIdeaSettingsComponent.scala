package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder

import java.io.File
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class RiddlIdeaSettingsComponent {
  private val confFilePath: Option[File] = None
  private val confFileText = ""
  private val confFileTextField = new JBTextField()

  var modified = false

  confFileTextField.getDocument.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit = {
      toggleModified()
    }
  })

  private def riddlMainPanel = FormBuilder.createFormBuilder
    .addLabeledComponent(
      "Enter conf file path relative to project base:",
      confFileTextField,
      1,
      false
    )
    .addComponentFillVertically(new JPanel(), 0)
    .getPanel

  def getPanel: JPanel = riddlMainPanel

  def getConfFieldText: String = confFileTextField.getText

  def toggleModified(): Unit = modified = !modified
}
