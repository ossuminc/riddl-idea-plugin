package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.editor.markup.{MarkupModel, RangeHighlighter}
import java.nio.file.Path
import com.intellij.openapi.components.{PersistentStateComponent, Storage, State as StateAnnotation}
import com.intellij.util.messages.MessageBusConnection
import com.ossuminc.riddl.language.Messages.Message

import com.intellij.openapi.components.{
  PersistentStateComponent,
  Storage,
  State as StateAnnotation
}
import com.ossuminc.riddl.utils.CommonOptions
import com.ossuminc.riddl.plugins.idea.utils.readFromOptionsFromConf

@StateAnnotation(
  name = "RiddlIdeaSettings",
  storages = Array(
    new Storage(
      "RiddlIdeaSettings.xml"
    )
  )
)

class RiddlIdeaSettings
    extends PersistentStateComponent[RiddlIdeaSettings.States] {
  private val states = new RiddlIdeaSettings.States()

  override def getState: RiddlIdeaSettings.States = states

  override def loadState(newStates: RiddlIdeaSettings.States): Unit = {
    states.load(newStates)
  }
}

object RiddlIdeaSettings {
  class States {
    private var states: Map[Int, State] = Map()

    def load(newStates: States): Unit = states = newStates.states

    def getState(numToolWindow: Int): State = states(numToolWindow)
    def allStates: Map[Int, State] = states

    def length: Int = states.size

    def newState(): Int = {
      val newWindowNum: Int =
        if length == 0 then 1
        else
          (1 to length + 1)
            .find(num => !states.keys.iterator.toSeq.contains(num))
            .getOrElse(length + 1)

      states = states.concat(Map(newWindowNum -> new State(newWindowNum)))
      newWindowNum
    }

    def removeState(numWindow: Int): Unit =
      states = states.view.filterKeys(_ != numWindow).toMap
  }

  class State(windowNum: Int) {
    private var riddlConfPath: Option[String] = None
    private var riddlTopLevelPath: Option[String] = None
    private var riddlRunOutput: Seq[String] = Seq()
    private var autoCompileOnSave: Boolean = true
    private var vfsConnection: Option[MessageBusConnection] = None
    private var command: String = commands.head
    private var commonOptions: CommonOptions = CommonOptions.empty.copy(
      noANSIMessages = true,
      groupMessagesByKind = true
    )
    private var fromOption: Option[String] = None
    private var fromOptionsSeq: Seq[String] =
      if riddlConfPath.isDefined then
        riddlConfPath.flatMap(readFromOptionsFromConf).toSeq
      else Seq()

    private var messagesForEditor: Seq[Message] = Seq()
    private var errorHighlighters: Seq[RangeHighlighter] = Seq()
    private var markupModelOpt: Option[MarkupModel] = None

    def getWindowNum: Int = windowNum

    def setConfPath(newPath: Option[String]): Unit = riddlConfPath = newPath
    def getConfPath: Option[String] = riddlConfPath

    def setTopLevelPath(newPath: String): Unit = riddlTopLevelPath = Some(
      newPath
    )
    def getTopLevelPath: Option[String] = riddlTopLevelPath

    def prependRunOutput(newOutput: String): Unit = riddlRunOutput =
      newOutput +: riddlRunOutput
    def appendRunOutput(newOutput: String): Unit = riddlRunOutput :+= newOutput
    def clearRunOutput(): Unit = riddlRunOutput = Seq()
    def getRunOutput: Seq[String] = riddlRunOutput

    def setAutoCompile(value: Boolean): Unit = autoCompileOnSave = value
    def getAutoCompile: Boolean = autoCompileOnSave

    def setVFSConnection(connection: MessageBusConnection): Unit = vfsConnection = Some(connection)
    def disconnectVFSListener: Unit = vfsConnection.foreach(_.disconnect())

    def setCommand(newCommand: String): Unit =
      if commands.contains(newCommand) then command = newCommand
    def getCommand: String = command

    def getCommonOptions: CommonOptions = commonOptions
    def setCommonOptions(newCOs: CommonOptions): Unit =
      commonOptions = newCOs

    def getMessagesForEditor: Seq[Message] = messagesForEditor
    def setMessagesForEditor(newMsgs: Seq[Message]): Unit =
      messagesForEditor = newMsgs

    def appendErrorHighlighter(
        rangeHighlighter: RangeHighlighter
    ): Seq[RangeHighlighter] =
      errorHighlighters :+ rangeHighlighter
    def clearErrorHighlighters(): Unit = {
      markupModelOpt.foreach(mm =>
        errorHighlighters.foreach(mm.removeHighlighter)
      )
      errorHighlighters = Seq()
    }

    def setMarkupModel(newModel: MarkupModel): Unit = markupModelOpt = Some(
      newModel
    )

    def setFromOption(newFromOption: String): Unit = fromOption = Some(
      newFromOption
    )
    def getFromOption: Option[String] = fromOption

    def setFromOptionsSeq(newSeq: Seq[String]): Unit = fromOptionsSeq = newSeq
    def getFromOptionsSeq: Seq[String] = fromOptionsSeq
  }

  private val commands = Seq("from", "about", "info")
  def allCommands: Seq[String] = commands
}
