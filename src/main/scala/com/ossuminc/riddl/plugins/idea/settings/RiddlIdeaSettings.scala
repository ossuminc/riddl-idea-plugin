package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.components.{PersistentStateComponent, RoamingType, State, Storage, StoragePathMacros}
import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState

@State(
  name = "RiddlIdeaSettings",
  storages = Array(
    new Storage(
      value = StoragePathMacros.MODULE_FILE,
      roamingType = RoamingType.DISABLED
    )
  )
)

class RiddlIdeaSettings
    extends PersistentStateComponent[RiddlIdeaSettings.State] {
  private var state = new RiddlIdeaSettings.State()

  override def getState: RiddlIdeaSettings.State = state

  override def loadState(newState: RiddlIdeaSettings.State): Unit = {
    state = newState
  }
}

object RiddlIdeaSettings {
  class State {
    var riddlConfPath: String = ""
    var riddlOutput: String = ""

    def setConfPath(newPath: String): Unit = {
      riddlConfPath = newPath
    }

    def appendOutput(newOutput: String): Unit = {
      riddlOutput += newOutput + "<br>"
    }

    def clearOutput(): Unit = {
      riddlOutput = ""
    }
  }

  def getInstance: RiddlIdeaSettings = getRiddlIdeaState
}
