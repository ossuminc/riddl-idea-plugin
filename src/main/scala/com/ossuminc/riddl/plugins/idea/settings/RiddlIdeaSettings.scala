package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.openapi.components.{PersistentStateComponent, RoamingType, State, Storage, StoragePathMacros}

@State(
  name = "RiddlIdeaSettings",
  storages = Array(
    new Storage(value = StoragePathMacros.MODULE_FILE, roamingType = RoamingType.DISABLED)
  )
)
final class RiddlIdeaSettings extends PersistentStateComponent[RiddlIdeaSettings.State] {
  private var state = new RiddlIdeaSettings.State()

  override def getState: RiddlIdeaSettings.State = state

  override def loadState(newState: RiddlIdeaSettings.State): Unit =
    state = new RiddlIdeaSettings.State(newState.riddlConfPath)
}

object RiddlIdeaSettings {
  class State(val riddlConfPath: String = "") {
    def apply(confPath: String): State = new State(confPath)
  }
}
