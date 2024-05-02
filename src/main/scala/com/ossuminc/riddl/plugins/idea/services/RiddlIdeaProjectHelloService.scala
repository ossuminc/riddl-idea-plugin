// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.ossuminc.riddl.plugins.idea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle
import org.jetbrains.annotations.NotNull

@Service
final class RiddlIdeaProjectHelloService(project: Project) {
  def getProjectHelloInfo: String =
    RiddlIdeaPluginBundle.message("com.ossuminc.riddl.plugins.idea", project.getName)
}

object RiddlIdeaProjectHelloService {
  def getInstance(@NotNull project: Project): RiddlIdeaProjectHelloService =
    project.getService(classOf[RiddlIdeaProjectHelloService])
}