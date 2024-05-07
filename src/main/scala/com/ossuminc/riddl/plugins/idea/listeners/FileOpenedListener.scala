// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.ossuminc.riddl.plugins.idea.listeners

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.fileEditor.{
  FileEditorManager,
  FileEditorManagerListener
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea
import com.ossuminc.riddl.plugins.idea.RiddlIdeaPluginBundle

class FileOpenedListener extends FileEditorManagerListener {
  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = {
    Notifications.Bus.notify(
      new Notification(
        "Riddl Plugin Notification",
        RiddlIdeaPluginBundle.message("file.opened"),
        RiddlIdeaPluginBundle.message("name.getname", file.getName),
        NotificationType.INFORMATION
      )
    )
  }
}
