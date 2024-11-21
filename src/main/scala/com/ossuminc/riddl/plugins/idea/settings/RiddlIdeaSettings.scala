package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.filePrediction.features.history.FileHistoryManagerWrapper.EditorManagerListener
import com.intellij.internal.sandbox.Highlighter
import com.intellij.openapi.components.{
  PersistentStateComponent,
  Storage,
  State as StateAnnotation
}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.{
  HighlighterTargetArea,
  MarkupModel,
  RangeHighlighter
}
import com.intellij.openapi.fileEditor.FileEditorManager
import com.ossuminc.riddl.utils.CommonOptions
import com.ossuminc.riddl.plugins.idea.utils.readFromOptionsFromConf
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getProject
import scala.collection.mutable._

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
    private var riddlRunOutput: scala.collection.mutable.Seq[String] =
      scala.collection.mutable.Seq()
    private var autoCompileOnSave: Boolean = true
    private var command: String = commands.head
    private var commonOptions: CommonOptions = CommonOptions.empty.copy(
      noANSIMessages = true,
      groupMessagesByKind = true
    )
    private var fromOption: Option[String] = None
    private var fromOptionsSeq: scala.collection.mutable.Seq[String] =
      if riddlConfPath.isDefined then
        scala.collection.mutable.Seq
          .from(riddlConfPath.flatMap(readFromOptionsFromConf).toSeq)
      else scala.collection.mutable.Seq()
    private val highlightersPerFile
        : scala.collection.mutable.Map[String, scala.collection.mutable.Seq[
          HighlighterInfo
        ]] = scala.collection.mutable.Map.empty

    def getWindowNum: Int = windowNum

    def setConfPath(newPath: Option[String]): Unit = riddlConfPath = newPath
    def getConfPath: Option[String] = riddlConfPath

    def prependRunOutput(newOutput: String): Unit = riddlRunOutput =
      newOutput +: riddlRunOutput
    def appendRunOutput(newOutput: String): Unit = riddlRunOutput :+= newOutput
    def clearRunOutput(): Unit = riddlRunOutput = scala.collection.mutable.Seq()
    def getRunOutput: scala.collection.mutable.Seq[String] = riddlRunOutput

    def setAutoCompile(value: Boolean): Unit = autoCompileOnSave = value
    def getAutoCompile: Boolean = autoCompileOnSave

    def setCommand(newCommand: String): Unit =
      if commands.contains(newCommand) then command = newCommand
    def getCommand: String = command

    def getCommonOptions: CommonOptions = commonOptions
    def setCommonOptions(newCOs: CommonOptions): Unit = {
      commonOptions = newCOs
    }

    def setFromOption(newFromOption: String): Unit = fromOption = Some(
      newFromOption
    )
    def getFromOption: Option[String] = fromOption

    def setFromOptionsSeq(newSeq: scala.collection.mutable.Seq[String]): Unit = fromOptionsSeq = newSeq
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
    ): Seq[HighlighterInfo] =
      highlightersPerFile.getOrElse(fileName, Seq())

    def clearHighlightersForFile(fileName: String): Unit = {
      println(highlightersPerFile)
      println(fileName)
      highlightersPerFile -= fileName
    }
  }

  private val commands = Seq("from", "about", "info")
  def allCommands: Seq[String] = commands
}
