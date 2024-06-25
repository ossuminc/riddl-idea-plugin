package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.util.messages.Topic

trait ChangeActionNotifier {}

object ChangeActionNotifier {
  @Topic.ProjectLevel
  val UPDATE_TOOL_WINDOW_TOPIC: Topic[ChangeActionNotifier] =
    Topic.create("updateRiddlToolWindow", classOf[ChangeActionNotifier])

}
