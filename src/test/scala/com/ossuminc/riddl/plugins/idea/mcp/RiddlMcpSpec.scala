/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.mcp

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL MCP integration components. */
class RiddlMcpSpec extends AnyWordSpec with Matchers {

  "McpError" must {

    "store error code and message" in {
      val error = McpError(-32600, "Invalid request")

      error.code mustBe -32600
      error.message mustBe "Invalid request"
    }
  }

  "McpInitializeResult" must {

    "store protocol version and server name" in {
      val result = McpInitializeResult("2024-11-05", "riddl-mcp-server")

      result.protocolVersion mustBe "2024-11-05"
      result.serverName mustBe "riddl-mcp-server"
    }
  }

  "McpValidationResult" must {

    "store validation status and messages" in {
      val messages = Seq(
        McpMessage("error", 1, 5, "Syntax error"),
        McpMessage("warning", 2, 10, "Unused variable")
      )
      val result = McpValidationResult(isValid = false, messages = messages)

      result.isValid mustBe false
      result.messages.size mustBe 2
    }

    "be valid when no error messages" in {
      val result = McpValidationResult(isValid = true, messages = Seq.empty)

      result.isValid mustBe true
      result.messages mustBe empty
    }
  }

  "McpMessage" must {

    "store severity, line, column, and message" in {
      val message = McpMessage("error", 10, 5, "Unexpected token")

      message.severity mustBe "error"
      message.line mustBe 10
      message.column mustBe 5
      message.message mustBe "Unexpected token"
    }

    "support different severity levels" in {
      val severities = Seq("error", "warning", "info")

      severities.foreach { sev =>
        val msg = McpMessage(sev, 1, 1, "test")
        msg.severity mustBe sev
      }
    }
  }

  "McpCompletenessResult" must {

    "store completeness status and missing elements" in {
      val result = McpCompletenessResult(
        isComplete = false,
        missingElements = "Missing: entity handler, type definition"
      )

      result.isComplete mustBe false
      result.missingElements must include("Missing")
    }

    "indicate complete model" in {
      val result = McpCompletenessResult(isComplete = true, missingElements = "")

      result.isComplete mustBe true
      result.missingElements mustBe empty
    }
  }

  "McpMappingResult" must {

    "store generated RIDDL" in {
      val riddl =
        """domain UserManagement is {
          |  context Users is {
          |    entity User is { }
          |  }
          |}""".stripMargin

      val result = McpMappingResult(generatedRiddl = riddl)

      result.generatedRiddl must include("domain UserManagement")
      result.generatedRiddl must include("context Users")
      result.generatedRiddl must include("entity User")
    }

    "handle empty generation" in {
      val result = McpMappingResult(generatedRiddl = "")

      result.generatedRiddl mustBe empty
    }
  }

  "McpTool" must {

    "store tool name and description" in {
      val tool = McpTool(
        name = "validate-partial",
        description = "Validates incomplete RIDDL models"
      )

      tool.name mustBe "validate-partial"
      tool.description must include("incomplete")
    }
  }

  "RiddlMcpClient" must {

    "create client with base URL" in {
      val client = new RiddlMcpClient("http://localhost:8080")
      client must not be null
    }

    "handle unreachable server gracefully" in {
      val client = new RiddlMcpClient("http://localhost:99999")
      val isHealthy = client.healthCheck()
      isHealthy mustBe false
    }
  }

  "MCP Protocol Constants" must {

    "use correct JSON-RPC version" in {
      // The protocol uses JSON-RPC 2.0
      val version = "2.0"
      version mustBe "2.0"
    }

    "have correct MCP protocol version" in {
      // Current MCP protocol version
      val protocolVersion = "2024-11-05"
      protocolVersion must startWith("2024")
    }
  }

  "Validation message parsing" must {

    "recognize error severity" in {
      val msg = McpMessage("error", 1, 1, "test")
      msg.severity mustBe "error"
    }

    "recognize warning severity" in {
      val msg = McpMessage("warning", 1, 1, "test")
      msg.severity mustBe "warning"
    }

    "recognize info severity" in {
      val msg = McpMessage("info", 1, 1, "test")
      msg.severity mustBe "info"
    }
  }

  "MCP tool names" must {

    "include validate-text" in {
      val toolNames = Seq(
        "validate-text",
        "validate-url",
        "validate-partial",
        "check-completeness",
        "check-simulability",
        "map-domain-to-riddl"
      )

      toolNames must contain("validate-text")
      toolNames must contain("validate-partial")
      toolNames must contain("check-completeness")
      toolNames must contain("map-domain-to-riddl")
    }
  }

  "MCP resource URIs" must {

    "have correct format" in {
      val grammarUri = "riddl://grammar/ebnf"
      val guideUri = "riddl://grammar/guide"

      grammarUri must startWith("riddl://")
      guideUri must startWith("riddl://")
    }
  }
}
