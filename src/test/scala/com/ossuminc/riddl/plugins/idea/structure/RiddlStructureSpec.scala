/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.structure

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL structure view components. */
class RiddlStructureSpec extends AnyWordSpec with Matchers {

  "RiddlStructureParser" must {

    "parse empty text" in {
      val result = RiddlStructureParser.parseDefinitions("")
      result mustBe empty
    }

    "parse a single domain" in {
      val text = "domain TestDomain is { }"
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      result.head.kind mustBe "domain"
      result.head.name mustBe "TestDomain"
    }

    "parse a domain with context" in {
      val text =
        """domain TestDomain is {
          |  context TestContext is { }
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      result.head.kind mustBe "domain"
      result.head.name mustBe "TestDomain"
      result.head.children.size mustBe 1
      result.head.children.head.kind mustBe "context"
      result.head.children.head.name mustBe "TestContext"
    }

    "parse nested entity within context" in {
      val text =
        """domain TestDomain is {
          |  context TestContext is {
          |    entity TestEntity is { }
          |  }
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      val domain = result.head
      domain.children.size mustBe 1
      val context = domain.children.head
      context.kind mustBe "context"
      // Note: Current implementation only shows direct children (level 1)
    }

    "parse multiple domains" in {
      val text =
        """domain Domain1 is { }
          |domain Domain2 is { }""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 2
      result(0).name mustBe "Domain1"
      result(1).name mustBe "Domain2"
    }

    "parse type definitions" in {
      val text =
        """domain TestDomain is {
          |  type UserId is String
          |  type UserName is String
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      val domain = result.head
      domain.children.size mustBe 2
      domain.children(0).kind mustBe "type"
      domain.children(0).name mustBe "UserId"
      domain.children(1).kind mustBe "type"
      domain.children(1).name mustBe "UserName"
    }

    "parse handler definitions" in {
      val text =
        """domain TestDomain is {
          |  context TestContext is {
          |    entity TestEntity is {
          |      handler TestHandler is { }
          |    }
          |  }
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      // Structure is hierarchical
    }

    "parse repository definitions" in {
      val text =
        """domain TestDomain is {
          |  context TestContext is {
          |    repository TestRepo is { }
          |  }
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      result.head.children.size mustBe 1
    }

    "parse saga definitions" in {
      val text =
        """domain TestDomain is {
          |  context TestContext is {
          |    saga TestSaga is { }
          |  }
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
    }

    "parse all definition kinds" in {
      // Test that all definition patterns are working
      val kinds = Seq(
        "domain",
        "context",
        "entity",
        "adaptor",
        "application",
        "epic",
        "saga",
        "repository",
        "projector",
        "streamlet",
        "connector",
        "handler",
        "function",
        "state",
        "type",
        "constant",
        "inlet",
        "outlet",
        "command",
        "event",
        "query",
        "result",
        "record"
      )

      kinds.foreach { kind =>
        val text = s"$kind TestName is { }"
        val result = RiddlStructureParser.parseDefinitions(text)
        result.nonEmpty mustBe true
        result.head.kind mustBe kind
        result.head.name mustBe "TestName"
      }
    }

    "handle malformed input gracefully" in {
      val text = "{ } { } domain"
      val result = RiddlStructureParser.parseDefinitions(text)
      // Should not throw exception
      result mustBe empty
    }

    "parse deep hierarchy via getTree" in {
      val text =
        """domain DeepTest is {
          |  context Outer is {
          |    entity Inner is {
          |      handler DeepHandler is { ??? }
          |    }
          |  }
          |}""".stripMargin
      val result = RiddlStructureParser.parseDefinitions(text)

      result.size mustBe 1
      val domain = result.head
      domain.kind mustBe "domain"
      domain.name mustBe "DeepTest"
      domain.children.size mustBe 1
      val context = domain.children.head
      context.kind mustBe "context"
      context.name mustBe "Outer"
      context.children.size mustBe 1
      val entity = context.children.head
      entity.kind mustBe "entity"
      entity.name mustBe "Inner"
      entity.children.size mustBe 1
      entity.children.head.kind mustBe "handler"
      entity.children.head.name mustBe "DeepHandler"
    }

    "fall back to regex for non-Root fragments" in {
      // Bare keyword not a valid Root — falls back to regex
      val text = "context Fragment is { }"
      val result = RiddlStructureParser.parseDefinitions(text)

      result.nonEmpty mustBe true
      result.head.kind mustBe "context"
      result.head.name mustBe "Fragment"
    }
  }

  "RiddlStructureIcons" must {

    "have an icon for FILE" in {
      RiddlStructureIcons.FILE must not be null
    }

    "have icons for all definition kinds" in {
      val kinds = Seq(
        "domain",
        "context",
        "entity",
        "adaptor",
        "application",
        "epic",
        "saga",
        "repository",
        "projector",
        "streamlet",
        "connector",
        "handler",
        "function",
        "state",
        "type",
        "constant",
        "inlet",
        "outlet",
        "command",
        "event",
        "query",
        "result",
        "record"
      )

      kinds.foreach { kind =>
        val icon = RiddlStructureIcons.forKind(kind)
        icon must not be null
      }
    }

    "return default icon for unknown kind" in {
      val icon = RiddlStructureIcons.forKind("unknownkind")
      icon mustBe RiddlStructureIcons.DEFAULT
    }
  }

  "RiddlDefinition" must {

    "store definition data correctly" in {
      val definition = RiddlDefinition("domain", "TestDomain", 0, 50)

      definition.kind mustBe "domain"
      definition.name mustBe "TestDomain"
      definition.offset mustBe 0
      definition.endOffset mustBe 50
      definition.children mustBe empty
    }

    "store children correctly" in {
      val child = RiddlDefinition("context", "TestContext", 10, 40)
      val parent = RiddlDefinition("domain", "TestDomain", 0, 50, Seq(child))

      parent.children.size mustBe 1
      parent.children.head mustBe child
    }
  }
}
