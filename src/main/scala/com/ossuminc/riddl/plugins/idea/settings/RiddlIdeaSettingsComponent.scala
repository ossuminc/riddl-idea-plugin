package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.fileChooser.{
  FileChooserDescriptor,
  FileChooserDescriptorFactory
}
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.{JBCheckBox, JBLabel, JBPanel, JBTextField}
import com.intellij.util.ui.{FormBuilder, JBDimension}
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle
import com.ossuminc.riddl.plugins.utils.{getProject, getRiddlIdeaState}

import java.awt.ComponentOrientation
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf")
  }
}

class RiddlIdeaSettingsComponent {
  private val confFileTextField = new TextFieldWithBrowseButton()

  confFileTextField.setText(
    if getRiddlIdeaState != null && !getRiddlIdeaState.getState.riddlConfPath.isBlank
    then getRiddlIdeaState.getState.riddlConfPath
    else getProject.getBasePath
  )

  var modified = false

  confFileTextField.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit = {
      modified = true
    }
  })

  private def createParamButton(
      param: String
  ): JBPanel[?] = {
    val row = new JBPanel()
    row.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)
    val checkBox = JBCheckBox()
    row.add(checkBox)
    val label = new JBLabel()
    label.setText(param)
    row.add(label)
    val textField = new JBTextField()
    row.add(textField)
    row
  }

  val button: JBPanel[?] = createParamButton("show-times")

  private def riddlMainPanel = FormBuilder.createFormBuilder
    .addLabeledComponent(
      "Current conf file path:",
      confFileTextField,
      1,
      false
    )
    .addComponentFillVertically(new JPanel(), 0)
    .addComponent(button)
    .getPanel

  riddlMainPanel.setMinimumSize(JBDimension(750, 500))

  private val fileDescriptor: FileChooserDescriptor =
    FileChooserDescriptorFactory
      .createSingleFileDescriptor()
      .withFileFilter(ConfCondition())

  confFileTextField.addBrowseFolderListener(
    RiddlIdeaPluginBundle.message(
      "riddl.plugins.idea.choose.conf.path"
    ),
    null,
    getProject,
    fileDescriptor
  )

  def getPanel: JPanel = riddlMainPanel

  def getConfFieldText: String = confFileTextField.getText
}
