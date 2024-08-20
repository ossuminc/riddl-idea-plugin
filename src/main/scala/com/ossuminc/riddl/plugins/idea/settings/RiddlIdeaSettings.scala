package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.components.{PersistentStateComponent, RoamingType, State, Storage}

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
    private var states: Seq[State] = Seq()

    def load(newStates: States): Unit = states = newStates.states

    def getState(numToolWindow: Int): State = if states.isEmpty then State(0)
      else states(numToolWindow - 1)

    def newState(): Unit = states :+= State(states.length + 1)

    def length: Int = states.length
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