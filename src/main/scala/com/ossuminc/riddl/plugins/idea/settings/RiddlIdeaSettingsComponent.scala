/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.fileChooser.{
  FileChooserDescriptor,
  FileChooserDescriptorFactory
}
import com.intellij.openapi.ui.{
  JBMenuItem,
  JBPopupMenu,
  TextComponentAccessor,
  TextFieldWithBrowseButton
}
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.{JBCheckBox, JBLabel, JBPanel}
import com.intellij.util.ui.FormBuilder
import com.ossuminc.riddl.plugins.idea.utils.readFromOptionsFromConf
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.settings.CommonOptionsUtils.{
  CommonOption,
  FiniteDurationCommonOption,
  IntegerCommonOption
}

import java.awt.{ComponentOrientation, FlowLayout}
import java.awt.event.{
  ActionEvent,
  ActionListener,
  ItemEvent,
  ItemListener,
  MouseAdapter,
  MouseEvent
}
import java.text.NumberFormat
import javax.swing.{
  BorderFactory,
  JFormattedTextField,
  JPanel,
  JTextField,
  SwingUtilities
}

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf") || fn.endsWith(".riddl")
  }
}

class RiddlCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".riddl")
  }
}

class RiddlIdeaSettingsComponent(private val numToolWindow: Int) {
  private val state: RiddlIdeaSettings.State = getRiddlIdeaState(numToolWindow)
  private var areAnyComponentsModified = false

  private var booleanCommonOptions: Seq[(JBCheckBox, CommonOption[Boolean])] =
    Seq()
  private def createBooleanParamButton(
      commonOption: CommonOption[Boolean]
  ): JBPanel[?] = {
    val row = new JBPanel(new FlowLayout(java.awt.FlowLayout.LEFT))
    row.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)

    val checkBox = JBCheckBox()
    if commonOption.getCommonOptionValue(state.getCommonOptions) then
      checkBox.doClick()
    row.add(checkBox)
    booleanCommonOptions = booleanCommonOptions :+ (checkBox, commonOption)
    checkBox.addItemListener(_ => areAnyComponentsModified = true)

    val label = new JBLabel()
    label.setText(commonOption.name)
    row.add(label)

    row
  }

  private var pickedCommandModified: Boolean = false
  private val commandPicker = JBLabel(state.getCommand)
  private val commandPickerPopupMenu = JBPopupMenu()

  private var pickedFromOptionModified: Boolean = false
  private val fromOptionPicker = JBLabel("A .conf file must be picked first")
  private val fromOptionPickerPopupMenu = JBPopupMenu()

  commandPicker.setBorder(
    BorderFactory.createTitledBorder("Choose run command")
  )
  fromOptionPicker.setBorder(
    BorderFactory.createTitledBorder("Choose from option")
  )

  private val topLevelFileTextField = new TextFieldWithBrowseButton()
  private val confFileTextField = new TextFieldWithBrowseButton()
  private val riddlFileDescriptor: FileChooserDescriptor =
    new FileChooserDescriptor(true, false, false, false, false, false)
      .withFileFilter(RiddlCondition())
  private val confFileDescriptor: FileChooserDescriptor =
    new FileChooserDescriptor(true, false, false, false, false, false)
      .withFileFilter(ConfCondition())

  topLevelFileTextField.addBrowseFolderListener(
    getProject,
    riddlFileDescriptor.withTitle("Browse for Top Level Path")
  )

  topLevelFileTextField.addPropertyChangeListener(_ =>
    areAnyComponentsModified = true
  )
  if state != null && state.getTopLevelPath.isDefined then
    state.getTopLevelPath.foreach(path =>
      topLevelFileTextField.setText(
        path
      )
    )
  else topLevelFileTextField.setText(getProject.getBasePath)

  topLevelFileTextField.setBorder(
    BorderFactory.createTitledBorder(
      "Select the project's top-level .riddl file [for editing]"
    )
  )

  confFileTextField.addBrowseFolderListener(
    getProject,
    confFileDescriptor.withTitle("Browse for Configuration Path")
  )
  confFileTextField.addPropertyChangeListener(_ =>
    areAnyComponentsModified = true
  )
  confFileTextField.setEditable(false)
  confFileTextField.setText(
    if state != null && state.getConfPath.isDefined then
      state.getConfPath.getOrElse("")
    else getProject.getBasePath
  )
  confFileTextField.setBorder(
    BorderFactory.createTitledBorder("Select .conf or .riddl file for console")
  )

  private val commonOptionsPanel: JPanel = new JPanel(
    new java.awt.GridLayout(0, 3)
  )
  commonOptionsPanel.setBorder(
    BorderFactory.createTitledBorder("Select CommonOptions for console")
  )
  CommonOptionsUtils.BooleanCommonOptions.foreach {
    case Some(option) =>
      commonOptionsPanel.add(createBooleanParamButton(option))
    case None =>
      commonOptionsPanel.add(
        new JBPanel(new FlowLayout(java.awt.FlowLayout.LEFT))
      )
  }
  private val integerOptionTextField = new JFormattedTextField(
    NumberFormat.getIntegerInstance()
  )
  integerOptionTextField.addPropertyChangeListener(_ =>
    areAnyComponentsModified = true
  )
  integerOptionTextField.setText(
    state.getCommonOptions.maxParallelParsing.toString
  )
  private val integerOptionRow = new JBPanel(
    new FlowLayout(java.awt.FlowLayout.LEFT)
  )
  integerOptionRow.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)
  private val integerOptionLabel = new JBLabel()
  integerOptionLabel.setText(IntegerCommonOption.name)
  integerOptionRow.add(integerOptionTextField)
  integerOptionRow.add(integerOptionLabel)
  commonOptionsPanel.add(integerOptionRow)

  private val finiteDurationTextField = new JFormattedTextField(
    NumberFormat.getIntegerInstance().setGroupingUsed(false)
  )
  finiteDurationTextField.addPropertyChangeListener(_ =>
    areAnyComponentsModified = true
  )
  finiteDurationTextField.setText(
    state.getCommonOptions.maxIncludeWait.toMillis.toString
  )
  private val finiteDurationOptionRow = new JBPanel(
    new FlowLayout(java.awt.FlowLayout.LEFT)
  )
  finiteDurationOptionRow.setComponentOrientation(
    ComponentOrientation.LEFT_TO_RIGHT
  )
  private val finiteDurationLabel = new JBLabel()
  finiteDurationLabel.setText(FiniteDurationCommonOption.name)
  finiteDurationOptionRow.add(finiteDurationTextField)
  finiteDurationOptionRow.add(finiteDurationLabel)
  commonOptionsPanel.add(finiteDurationOptionRow)

  private var autoParseValue: Boolean = state.getAutoParse

  private val riddlSettingsPanel = new JBPanel()

  private def createComponent(): Unit = {
    val riddlFormBuilder: FormBuilder = FormBuilder.createFormBuilder
      .addComponentFillVertically(new JPanel(), 0)
      .addComponent(topLevelFileTextField)
      .addComponent(
        commandPicker
      )
    val formBuilderPanel: JPanel = riddlFormBuilder.getPanel

    def setCommandPopupMenuListeners(): Unit = {
      RiddlIdeaSettings.allCommands.foreach { command =>
        val commandItem = new JBMenuItem(command)
        commandItem.addActionListener((_: ActionEvent) => {
          commandPicker.setText(command)
          pickedCommandModified = true
          riddlSettingsPanel.removeAll()
          createComponent()
          riddlSettingsPanel.repaint()
        })
        commandPickerPopupMenu.add(commandItem)
      }
    }

    state.getFromOptionsSeq
      .foreach { command =>
        val commandItem = new JBMenuItem(command)
        commandItem.addActionListener((_: ActionEvent) => {
          fromOptionPicker.setText(command)
          pickedFromOptionModified = true
        })
        fromOptionPickerPopupMenu.add(commandItem)
      }
    val fromOptionPickerListener = new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        riddlSettingsPanel.repaint()
        if SwingUtilities.isLeftMouseButton(e) then
          fromOptionPickerPopupMenu.show(fromOptionPicker, e.getX, e.getY)
      }
    }

    def reloadAndResetFromOptions(): Unit = {
      def newFromOptionPickerListener(): Unit =
        if fromOptionPicker.getMouseListeners.isEmpty then
          fromOptionPicker.addMouseListener(fromOptionPickerListener)
        else
          fromOptionPicker.removeMouseListener(fromOptionPickerListener)
          fromOptionPicker.addMouseListener(fromOptionPickerListener)

      def setFromOptionsPopupMenuListeners(): Unit =
        state.getFromOptionsSeq
          .foreach { command =>
            val commandItem = new JBMenuItem(command)
            commandItem.addActionListener((_: ActionEvent) => {
              fromOptionPicker.setText(command)
              pickedFromOptionModified = true
            })
            fromOptionPickerPopupMenu.add(commandItem)
          }

      if getConfFieldText.endsWith(".conf") then {
        state.setFromOptionsSeq(
          scala.collection.mutable.Seq.from(
            readFromOptionsFromConf(getConfFieldText).diff(Seq("common"))
          )
        )

        if fromOptionPickerPopupMenu.getComponents.count(
            _.isInstanceOf[JBMenuItem]
          ) != 0
        then
          fromOptionPickerPopupMenu.getComponents
            .filter(_.isInstanceOf[JBMenuItem])
            .foreach(fromOptionPickerPopupMenu.remove)

        setFromOptionsPopupMenuListeners()
        newFromOptionPickerListener()
        areAnyComponentsModified = true
        riddlSettingsPanel.repaint()
      }
    }

    if state.getFromOptionsSeq.nonEmpty then reloadAndResetFromOptions()

    val fileDescriptor: FileChooserDescriptor =
      new FileChooserDescriptor(true, false, false, false, false, false)
        .withFileFilter(ConfCondition())
    confFileTextField.addBrowseFolderListener(
      getProject,
      fileDescriptor.withTitle("Browse for Path"),
      new TextComponentAccessor[JTextField]:
        override def getText(component: JTextField): String = component.getText

        override def setText(component: JTextField, text: String): Unit = {
          component.setText(text)
          reloadAndResetFromOptions()
        }
    )

    val commandPickerListener = new MouseAdapter {
      override def mousePressed(e: MouseEvent): Unit =
        commandPickerPopupMenu.show(commandPicker, e.getX, e.getY)
    }

    if commandPicker.getMouseListeners.isEmpty then
      commandPicker.addMouseListener(commandPickerListener)
    else {
      commandPicker.addMouseListener(commandPickerListener)
      commandPicker.removeMouseListener(commandPickerListener)
    }

    if commandPickerPopupMenu.getComponents.count(
        _.isInstanceOf[JBMenuItem]
      ) != 0
    then
      commandPickerPopupMenu.getComponents
        .filter(_.isInstanceOf[JBMenuItem])
        .foreach(commandPickerPopupMenu.remove)

    setCommandPopupMenuListeners()
    riddlSettingsPanel.add(formBuilderPanel)

    riddlFormBuilder
      .addComponent(commonOptionsPanel)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel

    if commandPicker.getText == "from" then
      riddlFormBuilder
        .addComponentFillVertically(new JPanel(), 0)
        .addComponent(confFileTextField)
        .addComponentFillVertically(new JPanel(), 0)
        .addComponent(fromOptionPicker)

    val autoParseCheckBox: JBCheckBox = JBCheckBox()
    riddlFormBuilder
      .addComponentFillVertically(new JPanel(), 0)
      .addLabeledComponent(
        "Automatically re-parse on save",
        autoParseCheckBox
      )

    autoParseCheckBox.setSelected(autoParseValue)

    val autoParseListener = new ItemListener {
      def itemStateChanged(e: ItemEvent): Unit =
        autoParseValue = !autoParseValue
    }

    def newAutoParseListener(): Unit =
      autoParseCheckBox.addItemListener(autoParseListener)

    if !autoParseCheckBox.getItemListeners.isEmpty then
      autoParseCheckBox.getItemListeners.foreach(
        autoParseCheckBox.removeItemListener
      )
    newAutoParseListener()
  }

  createComponent()

  def getPanel: JPanel = riddlSettingsPanel

  def getTopLevelFieldText: String = topLevelFileTextField.getText
  def getConfFieldText: String = confFileTextField.getText

  def isModified: Boolean =
    areAnyComponentsModified ||
      pickedCommandModified || pickedFromOptionModified ||
      autoParseValue != state.getAutoParse

  def getPickedCommand: String = commandPicker.getText
  def getPickedFromOption: String = fromOptionPicker.getText

  def getAutoParseValue: Boolean = autoParseValue

  def getBooleanCommonOptions: Seq[(JBCheckBox, CommonOption[Boolean])] =
    booleanCommonOptions
  def getIntegerOptionTextField: JFormattedTextField = integerOptionTextField
  def getFiniteDurationOptionTextField: JFormattedTextField =
    finiteDurationTextField
}
