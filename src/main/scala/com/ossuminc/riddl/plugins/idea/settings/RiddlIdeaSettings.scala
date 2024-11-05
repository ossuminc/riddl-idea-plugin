package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.components.{PersistentStateComponent, Storage, State as StateAnnotation}
import com.ossuminc.riddl.utils.CommonOptions

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
      val newWindowNum: Int = if length == 0 then 1
        else (1 to length + 1)
          .find(num => !states.keys.iterator.toSeq.contains(num))
          .getOrElse(length + 1)

      states = states.concat(Map(newWindowNum -> new State(newWindowNum)))
      newWindowNum
    }

    def removeState(numWindow: Int): Unit =
      states = states.view.filterKeys(_ != numWindow).toMap
  }

  class State(windowNum: Int) {
    private var riddlConfPath: String = ""
    private var riddlRunOutput: Seq[String] = Seq()
    private var autoCompileOnSave: Boolean = true
    private var command: String = commands.head
    private var commonOptions: CommonOptions = CommonOptions.empty.copy(noANSIMessages = true, groupMessagesByKind = true)

    def getWindowNum: Int = windowNum
    
    def setConfPath(newPath: String): Unit = riddlConfPath = newPath
    def getConfPath: String = riddlConfPath

    def prependRunOutput(newOutput: String): Unit = riddlRunOutput = newOutput +: riddlRunOutput
    def appendRunOutput(newOutput: String): Unit = riddlRunOutput :+= newOutput
    def clearRunOutput(): Unit = riddlRunOutput = Seq()
    def getRunOutput: Seq[String] = riddlRunOutput

    def setAutoCompile(value: Boolean): Unit = autoCompileOnSave = value
    def getAutoCompile: Boolean = autoCompileOnSave

    def setCommand(newCommand: String): Unit =
      if commands.contains(newCommand) then command = newCommand
    def getCommand: String = command

    def getCommonOptions: CommonOptions = commonOptions
    def setCommonOptions(newCOs: CommonOptions): Unit = {
      commonOptions = newCOs
    }
  }

  private val commands = Seq("from", "about", "info")
  def allCommands: Seq[String] = commands
}