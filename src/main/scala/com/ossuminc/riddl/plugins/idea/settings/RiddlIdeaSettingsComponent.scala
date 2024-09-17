package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.fileChooser.{FileChooserDescriptor, FileChooserDescriptorFactory}
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.{JBCheckBox, JBLabel, JBPanel, JBTextField}
import com.intellij.util.ui.FormBuilder
import com.ossuminc.riddl.plugins.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.language.CommonOptions

import java.awt.ComponentOrientation
import java.awt.event.{ActionEvent, ActionListener, ItemEvent, ItemListener}
import javax.swing.JPanel
import javax.swing.event.{ChangeEvent, ChangeListener, DocumentEvent}

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf")
  }
}

class RiddlIdeaSettingsComponent(private val numToolWindow: Int) {
  private val confFileTextField = new TextFieldWithBrowseButton()
  private val autoCompileCheckBox = JBCheckBox()

  private val state = getRiddlIdeaState(numToolWindow)

  if state.getAutoCompile then autoCompileCheckBox.doClick()
  autoCompileCheckBox.addItemListener((e: ItemEvent) => state.toggleAutoCompile())

  confFileTextField.setText(
    if state != null && !state.getConfPath.isBlank then state.getConfPath
    else getProject.getBasePath
  )

  var modified = false

  confFileTextField.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit = {
      modified = true
    }
  })

  private def createParamButton(
    param: String,
    setCommonOption: CommonOptions => Boolean => CommonOptions,
    initialOptionState: CommonOptions => Boolean
  ): JBPanel[?] = {
    val row = new JBPanel()
    row.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)

    val checkBox = JBCheckBox()
    if initialOptionState(state.getCommonOptions) then checkBox.doClick()
    checkBox.addItemListener((e: ItemEvent) => {
      state.setCommonOptions(setCommonOption(state.getCommonOptions)(checkBox.isSelected))
    })
    row.add(checkBox)

    val label = new JBLabel()
    label.setText(param)
    row.add(label)

    row
  }

  import com.ossuminc.riddl.plugins.idea.settings.CommonOptionsUtils

  private val commonOptionsPanel: JPanel = new JPanel(new java.awt.GridLayout(0, 2))
  CommonOptionsUtils.AllCommonOptions.foreach(tup =>
    commonOptionsPanel.add(createParamButton(tup._1, tup._2, tup._3))
  )

  private val riddlMainPanel = FormBuilder.createFormBuilder
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
    .addLabeledComponent(
      "Common Options",
      commonOptionsPanel
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
