/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.navigation

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL navigation components. */
class RiddlNavigationSpec extends AnyWordSpec with Matchers {

  "RiddlNavigationUtils" must {

    "find references to an identifier" in {
      val text = "type UserId is String\ntype UserName is String\ntype User is { id: UserId }"
      val refs = RiddlNavigationUtils.findReferences(text, "UserId")

      refs.nonEmpty mustBe true
      refs.size mustBe 2 // Definition and reference
    }

    "find all occurrences of identifier" in {
      val text = "User User User"
      val refs = RiddlNavigationUtils.findReferences(text, "User")

      refs.size mustBe 3
    }

    "return empty for non-existent identifier" in {
      val text = "domain Test is { }"
      val refs = RiddlNavigationUtils.findReferences(text, "NotFound")

      refs mustBe empty
    }

    "find word boundaries correctly" in {
      val text = "UserId UserIdType"
      val refs = RiddlNavigationUtils.findReferences(text, "UserId")

      refs.size mustBe 1 // Should not match partial word
    }

    "detect definition position for domain" in {
      val text = "domain Test is { }"
      val isDefinition = RiddlNavigationUtils.isDefinitionPosition(text, 7) // At 'T' in Test

      isDefinition mustBe true
    }

    "detect definition position for type" in {
      val text = "type UserId is String"
      val isDefinition = RiddlNavigationUtils.isDefinitionPosition(text, 5) // At 'U' in UserId

      isDefinition mustBe true
    }

    "detect non-definition position" in {
      // Test on a line that doesn't start with a definition keyword
      val text = "// This is a comment with UserId reference"
      val isDefinition = RiddlNavigationUtils.isDefinitionPosition(text, 30)

      isDefinition mustBe false
    }

    "detect definition for all keyword types" in {
      val keywords = Seq(
        "domain",
        "context",
        "entity",
        "type",
        "handler",
        "state",
        "function",
        "repository",
        "saga",
        "projector",
        "streamlet",
        "connector",
        "adaptor",
        "application",
        "epic",
        "constant",
        "command",
        "event",
        "query",
        "result",
        "record",
        "inlet",
        "outlet"
      )

      keywords.foreach { kw =>
        val text = s"$kw TestName is"
        val isDefinition = RiddlNavigationUtils.isDefinitionPosition(text, kw.length + 1)
        isDefinition mustBe true
      }
    }
  }

  "RiddlGotoDeclarationHandler" must {

    "be instantiable" in {
      val handler = new RiddlGotoDeclarationHandler()
      handler must not be null
    }

    "return correct action text" in {
      val handler = new RiddlGotoDeclarationHandler()
      handler.getActionText(null) mustBe "Go to RIDDL Definition"
    }

    "handle null source element" in {
      val handler = new RiddlGotoDeclarationHandler()
      val result = handler.getGotoDeclarationTargets(null, 0, null)
      result mustBe null
    }
  }

  "Identifier extraction" must {

    "handle simple identifiers" in {
      val text = "domain TestDomain is { }"

      // Manually test identifier character logic
      def isIdentifierChar(c: Char): Boolean = c.isLetterOrDigit || c == '_'

      "TestDomain".forall(isIdentifierChar) mustBe true
      "Test_Domain".forall(isIdentifierChar) mustBe true
      "Test123".forall(isIdentifierChar) mustBe true
    }

    "reject non-identifier characters" in {
      def isIdentifierChar(c: Char): Boolean = c.isLetterOrDigit || c == '_'

      isIdentifierChar(' ') mustBe false
      isIdentifierChar('{') mustBe false
      isIdentifierChar(':') mustBe false
      isIdentifierChar('-') mustBe false
    }
  }

  "Definition finding" must {

    "find domain definitions" in {
      val text = "domain MyDomain is { }"
      val refs = RiddlNavigationUtils.findReferences(text, "MyDomain")
      refs.nonEmpty mustBe true
    }

    "find type definitions" in {
      val text = "type UserId is String"
      val refs = RiddlNavigationUtils.findReferences(text, "UserId")
      refs.nonEmpty mustBe true
    }

    "find entity definitions" in {
      val text = "entity User is { }"
      val refs = RiddlNavigationUtils.findReferences(text, "User")
      refs.nonEmpty mustBe true
    }

    "find handler definitions" in {
      val text = "handler UserHandler is { }"
      val refs = RiddlNavigationUtils.findReferences(text, "UserHandler")
      refs.nonEmpty mustBe true
    }
  }

  "Cross-reference resolution" must {

    "find type references in field declarations" in {
      val text =
        """type UserId is String
          |type User is {
          |  id: UserId
          |}""".stripMargin

      val refs = RiddlNavigationUtils.findReferences(text, "UserId")
      refs.size mustBe 2 // Definition and reference
    }

    "find entity references" in {
      val text =
        """entity User is { }
          |entity Order is {
          |  user: User.Id
          |}""".stripMargin

      val refs = RiddlNavigationUtils.findReferences(text, "User")
      refs.size mustBe 2
    }
  }
}
