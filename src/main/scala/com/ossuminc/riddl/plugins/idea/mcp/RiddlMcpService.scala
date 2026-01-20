/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings

import java.util.UUID
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

/** Service for managing MCP server communication.
  *
  * Provides a high-level API for RIDDL AI assistance features.
  */
@Service(Array(Service.Level.PROJECT))
final class RiddlMcpService(project: Project) {

  private val log = Logger.getInstance(classOf[RiddlMcpService])

  private val client: AtomicReference[Option[RiddlMcpClient]] = new AtomicReference(None)
  private val sessionId: AtomicReference[String] = new AtomicReference(generateSessionId())
  private val connected: AtomicBoolean = new AtomicBoolean(false)
  private val initialized: AtomicBoolean = new AtomicBoolean(false)

  /** Get the current connection status. */
  def isConnected: Boolean = connected.get()

  /** Get the current session ID. */
  def getSessionId: String = sessionId.get()

  /** Connect to the MCP server. */
  def connect(): Either[McpError, Unit] = {
    val settings = RiddlIdeaSettings.getInstance()
    val serverUrl = settings.getMcpServerUrl

    if serverUrl.isEmpty then {
      Left(McpError(-1, "MCP server URL not configured"))
    } else {
      val newClient = new RiddlMcpClient(serverUrl)

      // Check health first
      if !newClient.healthCheck() then {
        Left(McpError(-1, s"Cannot connect to MCP server at $serverUrl"))
      } else {
        client.set(Some(newClient))
        connected.set(true)

        // Initialize session
        newClient.initialize(sessionId.get()) match {
          case Right(result) =>
            initialized.set(true)
            log.info(s"Connected to MCP server: ${result.serverName} (${result.protocolVersion})")
            Right(())
          case Left(error) =>
            log.warn(s"MCP initialization failed: ${error.message}")
            // Still consider connected even if init fails
            Right(())
        }
      }
    }
  }

  /** Disconnect from the MCP server. */
  def disconnect(): Unit = {
    client.set(None)
    connected.set(false)
    initialized.set(false)
    sessionId.set(generateSessionId())
  }

  /** Validate RIDDL text using the MCP server.
    *
    * @param text     The RIDDL source text to validate
    * @param partial  If true, uses validate-partial (filters resolution errors)
    * @return Validation result or error
    */
  def validate(text: String, partial: Boolean = false): Either[McpError, McpValidationResult] =
    withClient { c =>
      if partial then c.validatePartial(text, sessionId.get())
      else c.validateText(text, sessionId.get())
    }

  /** Check model completeness using the MCP server.
    *
    * @param text The RIDDL source text to check
    * @return Completeness result or error
    */
  def checkCompleteness(text: String): Either[McpError, McpCompletenessResult] =
    withClient { c =>
      c.checkCompleteness(text, sessionId.get())
    }

  /** Generate RIDDL from a natural language description.
    *
    * @param description Natural language description of the domain
    * @return Generated RIDDL or error
    */
  def generateFromDescription(description: String): Either[McpError, McpMappingResult] =
    withClient { c =>
      c.mapDomainToRiddl(description, sessionId.get())
    }

  /** List available tools from the MCP server. */
  def listTools(): Either[McpError, Seq[McpTool]] =
    withClient { c =>
      c.listTools(sessionId.get())
    }

  /** Execute an operation with the client, handling connection state. */
  private def withClient[T](operation: RiddlMcpClient => Either[McpError, T]): Either[McpError, T] =
    client.get() match {
      case Some(c) => operation(c)
      case None =>
        // Try to auto-connect
        connect() match {
          case Right(_) =>
            client.get() match {
              case Some(c) => operation(c)
              case None    => Left(McpError(-1, "Failed to establish connection"))
            }
          case Left(error) => Left(error)
        }
    }

  private def generateSessionId(): String =
    s"idea-${UUID.randomUUID().toString.take(8)}"
}

object RiddlMcpService {

  /** Get the MCP service instance for a project. */
  def getInstance(project: Project): RiddlMcpService =
    project.getService(classOf[RiddlMcpService])
}
