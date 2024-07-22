package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.fileChooser.{
  FileChooserDescriptor,
  FileChooserDescriptorFactory
}
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.FormBuilder
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle
import com.ossuminc.riddl.plugins.utils.{getProject, getRiddlIdeaState}

import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf")
  }
}

class ExeCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    !fn.contains(".")
  }
}

class RiddlIdeaSettingsComponent {
  private val confFileTextField = new TextFieldWithBrowseButton()
  private val exeFileTextField = new TextFieldWithBrowseButton()

  confFileTextField.setText(
    if getRiddlIdeaState != null && !getRiddlIdeaState.getState.riddlConfPath.isBlank
    then getRiddlIdeaState.getState.riddlConfPath
    else getProject.getBasePath
  )

  confFileTextField.setText(
    if getRiddlIdeaState != null && !getRiddlIdeaState.getState.riddlExePath.isBlank
    then getRiddlIdeaState.getState.riddlExePath
    else ""
  )

  var modified: Boolean = false

  confFileTextField.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit =
      modified =
        getRiddlIdeaState.getState.riddlConfPath != confFileTextField.getText
  })

  exeFileTextField.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit =
      modified =
        getRiddlIdeaState.getState.riddlExePath != confFileTextField.getText
  })

  private def riddlMainPanel = FormBuilder.createFormBuilder
    .addLabeledComponent(
      "Current conf file path:",
      confFileTextField,
      1,
      false
    )
    .addLabeledComponent(
      "Current exe file path:",
      exeFileTextField,
      1,
      false
    )
    .addComponentFillVertically(new JPanel(), 0)
    .getPanel

  private val confFileDescriptor: FileChooserDescriptor =
    FileChooserDescriptorFactory
      .createSingleFileDescriptor()
      .withFileFilter(ConfCondition())

  private val exeFileDescriptor: FileChooserDescriptor =
    FileChooserDescriptorFactory
      .createSingleFileDescriptor()
      .withFileFilter(ExeCondition())

  confFileTextField.addBrowseFolderListener(
    RiddlIdeaPluginBundle.message(
      "riddl.plugins.idea.choose.conf.path"
    ),
    null,
    getProject,
    confFileDescriptor
  )

  exeFileTextField.addBrowseFolderListener(
    RiddlIdeaPluginBundle.message(
      "riddl.plugins.idea.choose.conf.path"
    ),
    null,
    getProject,
    exeFileDescriptor
  )

  def getPanel: JPanel = riddlMainPanel

  def getConfFieldText: String = confFileTextField.getText
  def getExeFieldText: String = exeFileTextField.getText
}
