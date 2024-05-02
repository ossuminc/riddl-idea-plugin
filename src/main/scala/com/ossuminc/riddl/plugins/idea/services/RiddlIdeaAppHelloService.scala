// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.ossuminc.riddl.plugins.idea.services

import com.intellij.openapi.application.{ApplicationInfo, ApplicationManager}
import com.intellij.openapi.components.Service
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle

@Service
final class RiddlIdeaAppHelloService {
  def getApplicationHelloInfo: String =
    RiddlIdeaPluginBundle.message(
      "hello.this.is.asstring",
      ApplicationInfo.getInstance().getBuild.asString()
    )
}

object RiddlIdeaAppHelloService {
  def getInstance: RiddlIdeaAppHelloService = ApplicationManager
    .getApplication
    .getService(classOf[RiddlIdeaAppHelloService])
}
