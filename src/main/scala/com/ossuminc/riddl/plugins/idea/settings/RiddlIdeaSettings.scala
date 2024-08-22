package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.components.{PersistentStateComponent, State, Storage}

import scala.collection.SortedMap

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
    private var states: SortedMap[Int, State] = SortedMap()

    def load(newStates: States): Unit = states = newStates.states

    def getStates: SortedMap[Int, State] = states
    def getState(numToolWindow: Int): State = states(numToolWindow)

    def length: Int = states.size

    def newState(): Int = {
      val newWindowNum: Int = if length == 0 then 0
        else if length == 1 then 2
        else (2 to length).find(num => !states.keys.iterator.contains(num)).getOrElse(length + 1)

      states = states.concat(Map(newWindowNum -> State(newWindowNum)))
      newWindowNum
    }

    def removeState(numWindow: Int): Unit =
      states = SortedMap[Int, State]() ++ states.view.filterKeys(_ != numWindow).toMap

    def nextNonConfiguredWindow: Int = states
      .map(state => state._1 -> state._2.areSettingsConfigured)
      .filter(state => state._2).keys.toSeq.headOption.getOrElse(-1)

  }

  class State(numToolWindow: Int) {
    var riddlConfPath: String = ""
    var riddlOutput: Seq[String] = Seq()
    var autoCompileOnSave: Boolean = true
    var areSettingsConfigured: Boolean = false

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