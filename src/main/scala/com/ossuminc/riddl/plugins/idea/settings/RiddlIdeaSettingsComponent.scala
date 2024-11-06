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
import com.intellij.ui.components.{JBCheckBox, JBLabel, JBPanel}
import com.intellij.util.ui.FormBuilder
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
import javax.swing.{BorderFactory, JFormattedTextField, JPanel, SwingUtilities}

class ConfCondition extends Condition[VirtualFile] {
  def value(virtualFile: VirtualFile): Boolean = {
    val fn = virtualFile.getName.toLowerCase
    fn.endsWith(".conf") || fn.endsWith(".riddl")
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
  private var pickedCommand: String = state.getCommand
  private val commandPicker = JBLabel(pickedCommand)
  private val commandPickerPopupMenu = JBPopupMenu()

  private var pickedFromOptionModified: Boolean = false
  private var pickedFromOption: String = state.getFromOption
  private val fromOptionPicker = JBLabel(pickedFromOption)
  private val fromOptionPickerPopupMenu = JBPopupMenu()

  commandPicker.setBorder(
    BorderFactory.createTitledBorder("Choose run command")
  )
  fromOptionPicker.setBorder(
    BorderFactory.createTitledBorder("Choose from option")
  )

  private val confFileTextField = new TextFieldWithBrowseButton()
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
  confFileTextField.addPropertyChangeListener(_ =>
    areAnyComponentsModified = true
  )
  confFileTextField.setText(
    if state != null && !state.getConfPath.isBlank then state.getConfPath
    else getProject.getBasePath
  )
  confFileTextField.setBorder(
    BorderFactory.createTitledBorder("Select .conf or .riddl File")
  )

  private val commonOptionsPanel: JPanel = new JPanel(
    new java.awt.GridLayout(0, 3)
  )
  commonOptionsPanel.setBorder(
    BorderFactory.createTitledBorder("Select Common Options")
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
  val finiteDurationlabel = new JBLabel()
  finiteDurationlabel.setText(FiniteDurationCommonOption.name)
  finiteDurationOptionRow.add(finiteDurationTextField)
  finiteDurationOptionRow.add(finiteDurationlabel)
  commonOptionsPanel.add(finiteDurationOptionRow)

  private var autoCompileValue: Boolean = state.getAutoCompile

  private val riddlPanel = new JBPanel()

  private def createComponent(): Unit = {
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
      .addComponent(
        commandPicker
      )

    val formBuilderPanel = riddlFormBuilder.getPanel

    def setCommandPopupMenuListeners(): Unit =
      RiddlIdeaSettings.allCommands.foreach { command =>
        val commandItem = new JBMenuItem(command)
        commandItem.addActionListener((_: ActionEvent) => {
          formBuilderPanel.removeAll()
          pickedCommand = command
          commandPicker.setText(command)
          pickedCommandModified = true
          createComponent()
          riddlPanel.revalidate()
          riddlPanel.repaint()
        })
        commandPickerPopupMenu.add(commandItem)
      }

    def setFromOptionsPopupMenuListeners(): Unit =
      RiddlIdeaSettings.allFromOptions.foreach { command =>
        val commandItem = new JBMenuItem(command)
        commandItem.addActionListener((_: ActionEvent) => {
          formBuilderPanel.removeAll()
          pickedFromOption = command
          fromOptionPicker.setText(command)
          pickedFromOptionModified = true
          createComponent()
          riddlPanel.revalidate()
          riddlPanel.repaint()
        })
        fromOptionPickerPopupMenu.add(commandItem)
      }

    if commandPickerPopupMenu.getComponents.count(
        _.isInstanceOf[JBMenuItem]
      ) != 0
    then
      commandPickerPopupMenu.getComponents
        .filter(_.isInstanceOf[JBMenuItem])
        .foreach(commandPickerPopupMenu.remove)

    if fromOptionPickerPopupMenu.getComponents.count(
        _.isInstanceOf[JBMenuItem]
      ) != 0
    then
      fromOptionPickerPopupMenu.getComponents
        .filter(_.isInstanceOf[JBMenuItem])
        .foreach(fromOptionPickerPopupMenu.remove)

    setCommandPopupMenuListeners()
    riddlPanel.add(formBuilderPanel)

    riddlFormBuilder
      .addComponent(commonOptionsPanel)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel

    if pickedCommand == "from" then {
      riddlFormBuilder
        .addComponentFillVertically(new JPanel(), 0)
        .addComponent(confFileTextField)

      val fromOptionPickerListener = new MouseAdapter {
        override def mouseClicked(e: MouseEvent): Unit =
          if SwingUtilities.isLeftMouseButton(e) then
            fromOptionPickerPopupMenu.show(fromOptionPicker, e.getX, e.getY)
      }

      def newFromOptionPickerListener(): Unit =
        fromOptionPicker.addMouseListener(fromOptionPickerListener)

      if fromOptionPicker.getMouseListeners.isEmpty then
        newFromOptionPickerListener()
      else
        fromOptionPicker.removeMouseListener(fromOptionPickerListener)
        newFromOptionPickerListener()

      riddlFormBuilder
        .addComponentFillVertically(new JPanel(), 0)
        .addComponent(fromOptionPicker)

      setFromOptionsPopupMenuListeners()
    }

    val autoCompileCheckBox: JBCheckBox = JBCheckBox()
    riddlFormBuilder
      .addComponentFillVertically(new JPanel(), 0)
      .addLabeledComponent(
        "Automatically re-compile on save",
        autoCompileCheckBox
      )

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
    areAnyComponentsModified ||
      pickedCommandModified || pickedFromOptionModified ||
      autoCompileValue != state.getAutoCompile

  def getPickedCommand: String = pickedCommand
  def getPickedFromOption: String = pickedFromOption

  def getAutoCompileValue: Boolean = autoCompileValue

  def getBooleanCommonOptions: Seq[(JBCheckBox, CommonOption[Boolean])] =
    booleanCommonOptions
  def getIntegerOptionTextField: JFormattedTextField = integerOptionTextField
  def getFiniteDurationOptionTextField: JFormattedTextField =
    finiteDurationTextField
}
