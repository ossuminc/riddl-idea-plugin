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
import com.ossuminc.riddl.language.CommonOptions
import com.ossuminc.riddl.plugins.idea.settings.CommonOptionsUtils.{
  CommonOption,
  FiniteDurationCommonOption,
  IntegerCommonOption
}
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*

import java.awt.{Color, ComponentOrientation, FlowLayout}
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

    val label = new JBLabel()
    label.setText(commonOption.name)
    row.add(label)

    row
  }

  private var pickedCommandModified: Boolean = false
  private var pickedCommand: String = state.getCommand
  private val commandPicker = JBLabel(pickedCommand)
  private val commandPickerPopupMenu = JBPopupMenu()

  private val integerOptionTextField = new JFormattedTextField(
    NumberFormat.getIntegerInstance()
  )
  private val finiteDurationTextField = new JFormattedTextField(
    NumberFormat.getIntegerInstance()
  )
  private val commonOptionsPanel: JPanel = new JPanel(
    new java.awt.GridLayout(0, 3)
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

  private var autoCompileValue: Boolean = state.getAutoCompile

  private val riddlPanel = new JBPanel()

  def createComponent(): Unit = {
    val commandPickerListener = new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit =
        if SwingUtilities.isLeftMouseButton(e) then
          commandPickerPopupMenu.show(commandPicker, e.getX, e.getY)
    }

    commandPicker.setBorder(
      BorderFactory.createTitledBorder("Choose Run Command")
    )
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

    def setPopupMenuListeners(): Unit = RiddlIdeaSettings.allCommands.foreach {
      command =>
        val commandItem = new JBMenuItem(command)
        commandItem.addActionListener((_: ActionEvent) => {
          formBuilderPanel.removeAll()
          commonOptionsPanel.removeAll()
          pickedCommand = command
          commandPicker.setText(command)
          pickedCommandModified = true
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

    commonOptionsPanel.setBorder(
      BorderFactory.createTitledBorder("Select Common Options")
    )
    CommonOptionsUtils.BooleanCommonOptions.foreach(option =>
      commonOptionsPanel.add(createBooleanParamButton(option))
    )

    val integerOptionRow = new JBPanel(new FlowLayout(java.awt.FlowLayout.LEFT))
    integerOptionRow.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT)

    val integerOptionLabel = new JBLabel()
    integerOptionTextField.setText(CommonOptions().maxParallelParsing.toString)
    integerOptionLabel.setText(IntegerCommonOption.name)
    integerOptionRow.add(integerOptionTextField)
    integerOptionRow.add(integerOptionLabel)
    commonOptionsPanel.add(integerOptionRow)

    val finiteDurationOptionRow = new JBPanel(
      new FlowLayout(java.awt.FlowLayout.LEFT)
    )
    finiteDurationOptionRow.setComponentOrientation(
      ComponentOrientation.LEFT_TO_RIGHT
    )

    val finiteDurationlabel = new JBLabel()
    finiteDurationTextField.setText(
      CommonOptions().maxIncludeWait.toMillis.toString
    )
    finiteDurationlabel.setText(FiniteDurationCommonOption.name)
    finiteDurationOptionRow.add(finiteDurationTextField)
    finiteDurationOptionRow.add(finiteDurationlabel)
    commonOptionsPanel.add(finiteDurationOptionRow)

//    PluginsDirOption

    confFileTextField.setText(
      if state != null && !state.getConfPath.isBlank then state.getConfPath
      else getProject.getBasePath
    )
    confFileTextField.setBorder(
      BorderFactory.createTitledBorder("Select .conf or .riddl File")
    )

    riddlFormBuilder
      .addComponent(commonOptionsPanel)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel

    if pickedCommand == "from" then
      riddlFormBuilder
        .addComponentFillVertically(new JPanel(), 0)
        .addComponent(confFileTextField)

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
    pickedCommandModified || autoCompileValue != state.getAutoCompile

  def getPickedCommand: String = pickedCommand

  def getAutoCompileValue: Boolean = autoCompileValue

  def getBooleanCommonOptions: Seq[(JBCheckBox, CommonOption[Boolean])] =
    booleanCommonOptions

  def getIntegerOptionTextField: JFormattedTextField = integerOptionTextField

  def getFiniteDurationOptionTextField: JFormattedTextField =
    finiteDurationTextField

  def getCommonOptionsPanel: JPanel = commonOptionsPanel

  def activateErrorBorder(): Unit = getCommonOptionsPanel.setBorder(
    BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.RED, 2),
      "Select Common Options"
    )
  )

  def activateRegularBorder(): Unit = getCommonOptionsPanel.setBorder(
    BorderFactory.createTitledBorder(
      "Select Common Options"
    )
  )
}
