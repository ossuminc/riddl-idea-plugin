package com.ossuminc.riddl.plugins.idea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.ossuminc.riddl.plugins.idea
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle
import org.jetbrains.annotations.NotNull

@Service
final class RiddlIdeaProjectHelloService(project: Project) {
  def getProjectHelloInfo: String =
    RiddlIdeaPluginBundle.message(
      "hello.from.project.getname",
      project.getName
    )
}

object RiddlIdeaProjectHelloService {
  def getInstance(@NotNull project: Project): RiddlIdeaProjectHelloService =
    project.getService(classOf[RiddlIdeaProjectHelloService])
}
