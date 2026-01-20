/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.mcp

import com.intellij.notification.{Notification, NotificationGroupManager, NotificationType}
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings

/** Action to connect to the MCP server. */
class McpConnectAction extends AnAction("Connect to MCP Server") {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    if project == null then return

    val mcpService = RiddlMcpService.getInstance(project)

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Connecting to MCP Server") {
      override def run(indicator: ProgressIndicator): Unit = {
        indicator.setText("Connecting to MCP server...")

        mcpService.connect() match {
          case Right(_) =>
            showNotification(project, "Connected to MCP server", NotificationType.INFORMATION)
          case Left(error) =>
            showNotification(project, s"Connection failed: ${error.message}", NotificationType.ERROR)
        }
      }
    })
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val settings = RiddlIdeaSettings.getInstance()

    e.getPresentation.setEnabled(
      project != null &&
        settings.getMcpEnabled &&
        !RiddlMcpService.getInstance(project).isConnected
    )
  }
}

/** Action to disconnect from the MCP server. */
class McpDisconnectAction extends AnAction("Disconnect from MCP Server") {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    if project == null then return

    val mcpService = RiddlMcpService.getInstance(project)
    mcpService.disconnect()
    showNotification(project, "Disconnected from MCP server", NotificationType.INFORMATION)
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject

    e.getPresentation.setEnabled(
      project != null && RiddlMcpService.getInstance(project).isConnected
    )
  }
}

/** Action to validate the current file using MCP. */
class McpValidateAction extends AnAction("Validate with MCP") {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = e.getData(CommonDataKeys.EDITOR)
    if project == null || editor == null then return

    val text = editor.getDocument.getText
    val mcpService = RiddlMcpService.getInstance(project)

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Validating with MCP") {
      override def run(indicator: ProgressIndicator): Unit = {
        indicator.setText("Validating RIDDL...")

        mcpService.validate(text, partial = true) match {
          case Right(result) =>
            val messageType =
              if result.isValid then NotificationType.INFORMATION
              else NotificationType.WARNING

            val message =
              if result.isValid then "Validation passed"
              else s"Validation found ${result.messages.size} issue(s)"

            showNotification(project, message, messageType)

          case Left(error) =>
            showNotification(project, s"Validation failed: ${error.message}", NotificationType.ERROR)
        }
      }
    })
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = e.getData(CommonDataKeys.EDITOR)
    val settings = RiddlIdeaSettings.getInstance()

    e.getPresentation.setEnabled(
      project != null &&
        editor != null &&
        settings.getMcpEnabled
    )
  }
}

/** Action to check model completeness using MCP. */
class McpCheckCompletenessAction extends AnAction("Check Completeness") {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = e.getData(CommonDataKeys.EDITOR)
    if project == null || editor == null then return

    val text = editor.getDocument.getText
    val mcpService = RiddlMcpService.getInstance(project)

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking Completeness") {
      override def run(indicator: ProgressIndicator): Unit = {
        indicator.setText("Checking model completeness...")

        mcpService.checkCompleteness(text) match {
          case Right(result) =>
            val messageType =
              if result.isComplete then NotificationType.INFORMATION
              else NotificationType.WARNING

            val message =
              if result.isComplete then "Model is complete"
              else s"Model is incomplete:\n${result.missingElements}"

            showNotification(project, message, messageType)

          case Left(error) =>
            showNotification(
              project,
              s"Completeness check failed: ${error.message}",
              NotificationType.ERROR
            )
        }
      }
    })
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    val editor = e.getData(CommonDataKeys.EDITOR)
    val settings = RiddlIdeaSettings.getInstance()

    e.getPresentation.setEnabled(
      project != null &&
        editor != null &&
        settings.getMcpEnabled
    )
  }
}

/** Show a notification balloon. */
private def showNotification(project: Project, message: String, notificationType: NotificationType): Unit = {
  ApplicationManager.getApplication.invokeLater(new Runnable {
    override def run(): Unit = {
      NotificationGroupManager
        .getInstance()
        .getNotificationGroup("Riddl Plugin Notification")
        .createNotification(message, notificationType)
        .notify(project)
    }
  })
}
