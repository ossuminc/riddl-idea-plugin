/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.mcp

import com.eclipsesource.json.{Json, JsonObject, JsonValue}

import java.net.{HttpURLConnection, URI}
import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter}
import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success, Try}

/** HTTP client for communicating with the RIDDL MCP server.
  *
  * Uses JSON-RPC 2.0 protocol over HTTP.
  */
class RiddlMcpClient(baseUrl: String) {

  private val requestIdCounter = new AtomicInteger(1)
  private val timeout = 30000 // 30 seconds

  /** Check if the MCP server is healthy. */
  def healthCheck(): Boolean =
    Try {
      val url = URI.create(s"$baseUrl/health").toURL
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("GET")
      connection.setConnectTimeout(5000)
      connection.setReadTimeout(5000)
      try {
        connection.getResponseCode == 200
      } finally {
        connection.disconnect()
      }
    }.getOrElse(false)

  /** Initialize an MCP session. */
  def initialize(sessionId: String): Either[McpError, McpInitializeResult] =
    callMethod(
      "initialize",
      Json
        .`object`()
        .add("protocolVersion", "2024-11-05")
        .add(
          "capabilities",
          Json.`object`().add("roots", Json.`object`().add("listChanged", true))
        )
        .add(
          "clientInfo",
          Json.`object`().add("name", "RIDDL IntelliJ Plugin").add("version", "1.0.0")
        ),
      sessionId
    ).map { result =>
      McpInitializeResult(
        protocolVersion = result.getString("protocolVersion", "unknown"),
        serverName = result.get("serverInfo").asObject().getString("name", "unknown")
      )
    }

  /** Call the validate-text tool. */
  def validateText(text: String, sessionId: String): Either[McpError, McpValidationResult] =
    callTool(
      "validate-text",
      Json.`object`().add("text", text),
      sessionId
    ).map(parseValidationResult)

  /** Call the validate-partial tool for incomplete models. */
  def validatePartial(text: String, sessionId: String): Either[McpError, McpValidationResult] =
    callTool(
      "validate-partial",
      Json.`object`().add("text", text),
      sessionId
    ).map(parseValidationResult)

  /** Call the check-completeness tool. */
  def checkCompleteness(text: String, sessionId: String): Either[McpError, McpCompletenessResult] =
    callTool(
      "check-completeness",
      Json.`object`().add("text", text),
      sessionId
    ).map { result =>
      val content = result.get("content").asArray()
      val text = if content.size() > 0 then content.get(0).asObject().getString("text", "") else ""
      McpCompletenessResult(
        isComplete = result.getBoolean("isComplete", false),
        missingElements = text
      )
    }

  /** Call the map-domain-to-riddl tool. */
  def mapDomainToRiddl(description: String, sessionId: String): Either[McpError, McpMappingResult] =
    callTool(
      "map-domain-to-riddl",
      Json.`object`().add("description", description),
      sessionId
    ).map { result =>
      val content = result.get("content").asArray()
      val text = if content.size() > 0 then content.get(0).asObject().getString("text", "") else ""
      McpMappingResult(generatedRiddl = text)
    }

  /** List available tools from the MCP server. */
  def listTools(sessionId: String): Either[McpError, Seq[McpTool]] =
    callMethod("tools/list", Json.`object`(), sessionId).map { result =>
      val tools = result.get("tools").asArray()
      (0 until tools.size()).map { i =>
        val tool = tools.get(i).asObject()
        McpTool(
          name = tool.getString("name", ""),
          description = tool.getString("description", "")
        )
      }
    }

  /** Call a specific MCP tool. */
  private def callTool(
      toolName: String,
      arguments: JsonObject,
      sessionId: String
  ): Either[McpError, JsonObject] =
    callMethod(
      "tools/call",
      Json.`object`().add("name", toolName).add("arguments", arguments),
      sessionId
    )

  /** Make a JSON-RPC method call. */
  private def callMethod(
      method: String,
      params: JsonObject,
      sessionId: String
  ): Either[McpError, JsonObject] = {
    val requestId = requestIdCounter.getAndIncrement()
    val request = Json
      .`object`()
      .add("jsonrpc", "2.0")
      .add("id", requestId)
      .add("method", method)
      .add("params", params)

    sendRequest(request, sessionId) match {
      case Right(response) =>
        if response.get("error") != null && !response.get("error").isNull then {
          val error = response.get("error").asObject()
          Left(
            McpError(
              code = error.getInt("code", -1),
              message = error.getString("message", "Unknown error")
            )
          )
        } else {
          val result = response.get("result")
          if result != null && result.isObject then Right(result.asObject())
          else Right(Json.`object`())
        }
      case Left(error) => Left(error)
    }
  }

  /** Send an HTTP request to the MCP server. */
  private def sendRequest(request: JsonObject, sessionId: String): Either[McpError, JsonObject] =
    Try {
      val url = URI.create(s"$baseUrl/mcp").toURL
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("X-Session-ID", sessionId)
      connection.setConnectTimeout(timeout)
      connection.setReadTimeout(timeout)
      connection.setDoOutput(true)

      try {
        // Send request
        val writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
        writer.write(request.toString)
        writer.flush()
        writer.close()

        // Read response
        val responseCode = connection.getResponseCode
        val inputStream =
          if responseCode >= 400 then connection.getErrorStream
          else connection.getInputStream
        val reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))
        val response = new StringBuilder
        var line: String = reader.readLine()
        while line != null do {
          response.append(line)
          line = reader.readLine()
        }
        reader.close()

        if responseCode >= 400 then Left(McpError(-1, s"HTTP $responseCode: ${response.toString}"))
        else Right(Json.parse(response.toString).asObject())
      } finally {
        connection.disconnect()
      }
    } match {
      case Success(result) => result
      case Failure(e)      => Left(McpError(-1, s"Connection failed: ${e.getMessage}"))
    }

  /** Parse a validation result from JSON. */
  private def parseValidationResult(result: JsonObject): McpValidationResult = {
    val content = result.get("content")
    val text =
      if content != null && content.isArray && content.asArray().size() > 0 then
        content.asArray().get(0).asObject().getString("text", "")
      else ""

    // Parse messages from the text (they're typically in a specific format)
    val messages = parseMessages(text)

    McpValidationResult(
      isValid = messages.forall(_.severity != "error"),
      messages = messages
    )
  }

  /** Parse validation messages from text output. */
  private def parseMessages(text: String): Seq[McpMessage] =
    if text.isEmpty then Seq.empty
    else {
      // Simple parsing - each line is a message
      // Format: [severity] at line:col - message
      val pattern = """(?i)\[(error|warning|info)\]\s+(?:at\s+)?(?:(\d+):(\d+)\s*-?\s*)?(.+)""".r
      text
        .split("\n")
        .flatMap { line =>
          pattern.findFirstMatchIn(line).map { m =>
            McpMessage(
              severity = m.group(1).toLowerCase,
              line = Option(m.group(2)).map(_.toInt).getOrElse(0),
              column = Option(m.group(3)).map(_.toInt).getOrElse(0),
              message = m.group(4).trim
            )
          }
        }
        .toSeq
    }
}

/** MCP error response. */
case class McpError(code: Int, message: String)

/** MCP initialization result. */
case class McpInitializeResult(protocolVersion: String, serverName: String)

/** MCP validation result. */
case class McpValidationResult(isValid: Boolean, messages: Seq[McpMessage])

/** A validation message. */
case class McpMessage(severity: String, line: Int, column: Int, message: String)

/** MCP completeness check result. */
case class McpCompletenessResult(isComplete: Boolean, missingElements: String)

/** MCP domain mapping result. */
case class McpMappingResult(generatedRiddl: String)

/** MCP tool description. */
case class McpTool(name: String, description: String)
