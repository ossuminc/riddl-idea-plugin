package com.ossuminc.riddl.plugins.idea.listeners

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.fileEditor.{
  FileEditorManager,
  FileEditorManagerListener
}
import com.intellij.openapi.vfs.VirtualFile
import com.ossuminc.riddl.plugins.idea

class FileOpenedListener extends FileEditorManagerListener {
  override def fileOpened(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = {
    Notifications.Bus.notify(
      new Notification(
        "Riddl Plugin Notification",
        "File opened: " + file.getName,
        NotificationType.INFORMATION
      )
    )
  }
}
