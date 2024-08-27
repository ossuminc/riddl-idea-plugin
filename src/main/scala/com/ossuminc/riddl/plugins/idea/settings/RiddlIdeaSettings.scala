package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.components.{PersistentStateComponent, State, Storage}

@State(
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

    def getStates: Map[Int, State] = states
    def getState(numToolWindow: Int): State = states(numToolWindow)

    def length: Int = states.size

    def newState(): Int = {
      val newWindowNum: Int = if length == 0 then 0
        else if length == 1 then 2
        else (2 to length)
          .find(num => !states.keys.iterator.toSeq.contains(num))
          .getOrElse(length + 1)

      states = states.concat(Map(newWindowNum -> State(newWindowNum)))
      println(states)
      newWindowNum
    }

    def removeState(numWindow: Int): Unit =
      states = states.view.filterKeys(_ != numWindow).toMap
  }

  class State(numToolWindow: Int) {
    var riddlConfPath: String = ""
    var riddlOutput: Seq[String] = Seq()
    var autoCompileOnSave: Boolean = true

    def setConfPath(newPath: String): Unit = {
      riddlConfPath = newPath
    }

    def appendOutput(newOutput: String): Unit = {
      riddlOutput :+= newOutput
    }

    def clearOutput(): Unit = {
      riddlOutput = Seq()
    }

    def toggleAutoCompile(): Unit = autoCompileOnSave = !autoCompileOnSave
  }
}