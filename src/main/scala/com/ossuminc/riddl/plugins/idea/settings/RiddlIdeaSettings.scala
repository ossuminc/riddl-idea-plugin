package com.ossuminc.riddl.plugins.idea.settings

import java.nio.file.Path
import com.intellij.util.messages.MessageBusConnection
import com.ossuminc.riddl.language.Messages.Message
import com.ossuminc.riddl.plugins.idea.utils.readFromOptionsFromConf

import com.intellij.openapi.editor.markup.{MarkupModel, RangeHighlighter}
import com.intellij.openapi.components.{
  PersistentStateComponent,
  Storage,
  State as StateAnnotation
}
import com.ossuminc.riddl.utils.CommonOptions

import scala.collection.mutable
import scala.collection.mutable.*

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

case class HighlighterInfo(startOffset: Int, endOffset: Int, layer: Int)

object HighlighterInfo {
  def fromRangeHighlighter(highlighter: RangeHighlighter): HighlighterInfo = {
    HighlighterInfo(
      highlighter.getStartOffset,
      highlighter.getEndOffset,
      highlighter.getLayer
    )
  }
}

object RiddlIdeaSettings {
  class States {
    private var states: scala.collection.mutable.Map[Int, State] =
      scala.collection.mutable.Map()

    def load(newStates: States): Unit = states = newStates.states

    def getState(numToolWindow: Int): State = states(numToolWindow)
    def allStates: scala.collection.mutable.Map[Int, State] = states

    def length: Int = states.size

    def newState(): Int = {
      val newWindowNum: Int =
        if length == 0 then 1
        else
          (1 to length + 1)
            .find(num => !states.keys.iterator.toSeq.contains(num))
            .getOrElse(length + 1)

      states = states.concat(
        scala.collection.mutable.Map(newWindowNum -> new State(newWindowNum))
      )
      newWindowNum
    }

    def removeState(numWindow: Int): Unit =
      states = scala.collection.mutable.Map
        .from(states.view.filterKeys(_ != numWindow).toMap)
  }

  class State(windowNum: Int) {
    private var riddlConfPath: Option[String] = None
    private var riddlTopLevelPath: Option[String] = None
    private var riddlRunOutput: scala.collection.mutable.Seq[String] =
      scala.collection.mutable.Seq()
    private var autoCompileOnSave: Boolean = true
    private var vfsConnection: Option[MessageBusConnection] = None
    private var command: String = commands.head
    private var commonOptions: CommonOptions = CommonOptions.empty.copy(
      noANSIMessages = true,
      groupMessagesByKind = true
    )
    private var fromOption: Option[String] = None
    private var fromOptionsSeq: mutable.Seq[String] =
      if riddlConfPath.isDefined then
        mutable.Seq
          .from(riddlConfPath.flatMap(readFromOptionsFromConf).toSeq)
      else mutable.Seq()
    private var highlightersPerFile: mutable.Map[String, mutable.Seq[
      HighlighterInfo
    ]] = mutable.Map.empty

    private var messagesForConsole: mutable.Seq[Message] = mutable.Seq()
    private var messagesForEditor: mutable.Seq[Message] = mutable.Seq()

    def getWindowNum: Int = windowNum

    def setConfPath(newPath: Option[String]): Unit = riddlConfPath = newPath
    def getConfPath: Option[String] = riddlConfPath

    def setTopLevelPath(newPath: String): Unit = riddlTopLevelPath = Some(
      newPath
    )
    def getTopLevelPath: Option[String] = riddlTopLevelPath

    def appendRunOutput(newOutput: String): Unit = riddlRunOutput :+= newOutput
    def clearRunOutput(): Unit = riddlRunOutput = scala.collection.mutable.Seq()
    def getRunOutput: scala.collection.mutable.Seq[String] = riddlRunOutput

    def setAutoCompile(value: Boolean): Unit = autoCompileOnSave = value
    def getAutoCompile: Boolean = autoCompileOnSave

    def setVFSConnection(connection: MessageBusConnection): Unit = vfsConnection = Some(connection)
    def disconnectVFSListener(): Unit = vfsConnection.foreach(_.disconnect())

    def setCommand(newCommand: String): Unit =
      if commands.contains(newCommand) then command = newCommand
    def getCommand: String = command

    def getCommonOptions: CommonOptions = commonOptions
    def setCommonOptions(newCOs: CommonOptions): Unit =
      commonOptions = newCOs

    def getMessagesForEditor: mutable.Seq[Message] = messagesForEditor
    def setMessagesForEditor(newMsgs: mutable.Seq[Message]): Unit =
      messagesForEditor = newMsgs
    def getMessagesForConsole: mutable.Seq[Message] = messagesForConsole
    def setMessagesForConsole(newMsgs: mutable.Seq[Message]): Unit =
      messagesForConsole = newMsgs

    def setFromOption(newFromOption: String): Unit = fromOption = Some(
      newFromOption
    )
    def getFromOption: Option[String] = fromOption

    def setFromOptionsSeq(newSeq: scala.collection.mutable.Seq[String]): Unit =
      fromOptionsSeq = newSeq
    def getFromOptionsSeq: scala.collection.mutable.Seq[String] = fromOptionsSeq

    def saveHighlighterForFile(
        fileName: String,
        highlighter: RangeHighlighter
    ): Unit = highlightersPerFile += (
      fileName -> (getHighlightersForFile(
        fileName
      ) :+ HighlighterInfo.fromRangeHighlighter(highlighter))
    )
    def getHighlightersForFile(
        fileName: String
    ): mutable.Seq[HighlighterInfo] =
      highlightersPerFile.getOrElse(fileName, mutable.Seq())
    def clearHighlightersForFile(fileName: String): Unit = {
      println(highlightersPerFile)
      println(fileName)
      highlightersPerFile -= fileName
    }
    def clearAllHighlighters(): Unit = highlightersPerFile = mutable.Map()
  }

  private val commands = mutable.Seq("from", "about", "info")
  def allCommands: mutable.Seq[String] = commands
}
