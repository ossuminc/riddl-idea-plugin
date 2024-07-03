package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState

import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class RiddlIdeaSettingsComponent {
  private val confFileTextField = new JBTextField()

  confFileTextField.setText(
    if getRiddlIdeaState != null then getRiddlIdeaState.getState.riddlConfPath
    else ""
  )

  var modified = false

  confFileTextField.getDocument.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit = {
      modified = true
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
}
