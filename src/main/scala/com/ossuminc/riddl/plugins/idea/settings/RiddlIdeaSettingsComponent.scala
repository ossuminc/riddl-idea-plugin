package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.fileChooser.{
  FileChooserDescriptor,
  FileChooserDescriptorFactory
}
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.{JBCheckBox, JBLabel, JBPanel}
import com.intellij.util.ui.FormBuilder
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*

import java.awt.event.{ItemEvent, ItemListener}
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf")
  }
}

class RiddlIdeaSettingsComponent(private val numToolWindow: Int) {
  private val confFileTextField = new TextFieldWithBrowseButton()
  private val autoCompileRow = new JBPanel()
  private val autoCompileCheckBox = JBCheckBox()
  private val autoCompileLabel = new JBLabel()

  private val state = getRiddlIdeaState(numToolWindow)

  autoCompileCheckBox.doClick()
  autoCompileCheckBox.addItemListener((e: ItemEvent) =>
    state.toggleAutoCompile()
  )
  println(state.riddlConfPath)
  confFileTextField.setText(
    if state != null && !state.riddlConfPath.isBlank then state.riddlConfPath
    else getProject.getBasePath
  )

  var modified = false

  confFileTextField.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit = {
      modified = true
    }
  })

  // private def createParamButton(
  //    param: String
  // ): JBPanel[?] = {
  //  val row = new JBPanel()
  //  row.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)
  //  val checkBox = JBCheckBox()
  //  row.add(checkBox)
  //  val label = new JBLabel()
  //  label.setText(param)
  //  row.add(label)
  //  val textField = new JBTextField()
  //  row.add(textField)
  //  row
  // }

  // val button: JBPanel[?] = createParamButton("show-times")

  private def riddlMainPanel = FormBuilder.createFormBuilder
    .addLabeledComponent(
      "Current conf file path:",
      confFileTextField,
      1,
      false
    )
    .addComponentFillVertically(new JPanel(), 0)
    .addLabeledComponent(
      "Automatically re-compile on save",
      autoCompileCheckBox
    )
    .addComponentFillVertically(new JPanel(), 0)
    .getPanel

  private val fileDescriptor: FileChooserDescriptor =
    FileChooserDescriptorFactory
      .createSingleFileDescriptor()
      .withFileFilter(ConfCondition())

  confFileTextField.addBrowseFolderListener(
    "Browse for Path",
    null,
    getProject,
    fileDescriptor
  )

  def getPanel: JPanel = riddlMainPanel

  def getConfFieldText: String = confFileTextField.getText
}
