package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.fileChooser.{
  FileChooserDescriptor,
  FileChooserDescriptorFactory
}
import com.intellij.openapi.ui.{
  JBMenuItem,
  JBPopupMenu,
  TextFieldWithBrowseButton
}
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.{JBCheckBox, JBLabel, JBPanel, JBTextField}
import com.intellij.util.ui.FormBuilder
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*

import java.awt.event.{
  ItemEvent,
  ItemListener,
  ActionEvent,
  MouseAdapter,
  MouseEvent
}
import javax.swing.{JPanel, SwingUtilities}
import javax.swing.event.DocumentEvent

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf")
  }
}

class RiddlIdeaSettingsComponent(private val numToolWindow: Int) {
  private val state: RiddlIdeaSettings.State = getRiddlIdeaState(numToolWindow)

  private val autoCompileCheckBox: JBCheckBox = JBCheckBox()
  private var autoCompileValue: Boolean = state.getAutoCompile

  private var confFieldModified: Boolean = false
  private val confFileTextField: TextFieldWithBrowseButton = {
    val confFileTextField = new TextFieldWithBrowseButton()
    confFileTextField.setText(
      if state != null && !state.getConfPath.isBlank then state.getConfPath
      else getProject.getBasePath
    )

    confFileTextField.addDocumentListener(new DocumentAdapter {
      override def textChanged(e: DocumentEvent): Unit = {
        confFieldModified = true
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

    confFileTextField
  }

  private var pickedCommandModified: Boolean = false
  private var pickedCommand: String = state.getCommand
  private val commandPicker = JBLabel(pickedCommand)
  private val commandPickerPopupMenu = JBPopupMenu()

  private val riddlPanel = new JBPanel()

  def createComponent(): Unit = {
    val commandPickerListener = new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit =
        if SwingUtilities.isLeftMouseButton(e) then
          commandPickerPopupMenu.show(commandPicker, e.getX, e.getY)
    }
    def newCommandPickerListener(): Unit =
      commandPicker.addMouseListener(commandPickerListener)

    if commandPicker.getMouseListeners.isEmpty then newCommandPickerListener()
    else
      commandPicker.removeMouseListener(commandPickerListener)
      newCommandPickerListener()

    val riddlFormBuilder: FormBuilder = FormBuilder.createFormBuilder
      .addLabeledComponent(
        "Command to run:",
        commandPicker
      )

    val formBuilderPanel = riddlFormBuilder.getPanel

    def setPopupMenuListeners(): Unit = RiddlIdeaSettings.allCommands.foreach {
      command =>
        val commandItem = new JBMenuItem(command)
        commandItem.addActionListener((_: ActionEvent) => {
          pickedCommand = command
          commandPicker.setText(command)
          pickedCommandModified = true
          riddlPanel.remove(formBuilderPanel)
          createComponent()
          riddlPanel.revalidate()
          riddlPanel.repaint()
        })
        commandPickerPopupMenu.add(commandItem)
    }

    if commandPickerPopupMenu.getComponents
        .count(
          _.isInstanceOf[JBMenuItem]
        ) != 0
    then
      commandPickerPopupMenu.getComponents
        .filter(_.isInstanceOf[JBMenuItem])
        .foreach(commandPickerPopupMenu.remove)

    setPopupMenuListeners()

    riddlPanel.add(formBuilderPanel)

    if pickedCommand == "from" then
      riddlFormBuilder.addLabeledComponent(
        "Current configuration file path:",
        confFileTextField
      )

    riddlFormBuilder
      .addComponentFillVertically(new JPanel(), 0)
      .addLabeledComponent(
        "Automatically re-compile on save",
        autoCompileCheckBox
      )
      .addComponentFillVertically(new JPanel(), 0)

    autoCompileCheckBox.setSelected(autoCompileValue)

    val autoCompileListener = new ItemListener {
      def itemStateChanged(e: ItemEvent): Unit =
        autoCompileValue = !autoCompileValue
    }

    def newAutoCompileListener(): Unit =
      autoCompileCheckBox.addItemListener(autoCompileListener)

    if !autoCompileCheckBox.getItemListeners.isEmpty then
      autoCompileCheckBox.getItemListeners.foreach(
        autoCompileCheckBox.removeItemListener
      )
    newAutoCompileListener()
  }

  createComponent()

  def getPanel: JPanel = riddlPanel

  def getConfFieldText: String = confFileTextField.getText

  def isModified: Boolean =
    confFieldModified || pickedCommandModified || autoCompileValue != state.getAutoCompile

  def getPickedCommand: String = pickedCommand

  def getAutoCompileValue: Boolean = autoCompileValue
}
