/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.folding

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL folding builder.
  *
  * These tests verify fold region detection patterns.
  * Full integration testing requires IntelliJ platform test fixtures.
  */
class RiddlFoldingBuilderSpec extends AnyWordSpec with Matchers {

  "RiddlFoldingBuilder" must {

    "have foldable patterns defined" in {
      RiddlFoldingBuilder.FOLDABLE_PATTERNS.nonEmpty mustBe true
    }

    "include pattern for domain" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("domain TestDomain").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for context" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("context TestContext").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for entity" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("entity TestEntity").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for handler" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("handler TestHandler").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for type" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("type TestType").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for state" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("state TestState").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for repository" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("repository TestRepo").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for saga" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("saga TestSaga").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for on command" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("on command").isDefined
      }
      hasPattern mustBe true
    }

    "include pattern for on event" in {
      val hasPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS.exists { case (pattern, _) =>
        pattern.findFirstIn("on event").isDefined
      }
      hasPattern mustBe true
    }

    "match domain keyword" in {
      val domainPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS
        .find { case (pattern, _) => pattern.findFirstIn("domain MyDomain").isDefined }
        .map(_._1)

      domainPattern.isDefined mustBe true
      // Pattern should match domain definitions
      domainPattern.get.findFirstIn("domain MyDomain").isDefined mustBe true
    }

    "match indented domain" in {
      val domainPattern = RiddlFoldingBuilder.FOLDABLE_PATTERNS
        .find { case (pattern, _) => pattern.findFirstIn("domain MyDomain").isDefined }
        .map(_._1)

      domainPattern.isDefined mustBe true
      // Pattern should match indented definitions too
      domainPattern.get.findFirstIn("  domain MyDomain").isDefined mustBe true
    }

    "return placeholder text" in {
      val builder = new RiddlFoldingBuilder()
      builder.getPlaceholderText(null) mustBe "..."
    }

    "not collapse by default" in {
      val builder = new RiddlFoldingBuilder()
      builder.isCollapsedByDefault(null) mustBe false
    }
  }

  "Foldable patterns" must {

    "all have placeholder text" in {
      RiddlFoldingBuilder.FOLDABLE_PATTERNS.foreach { case (_, placeholder) =>
        placeholder.nonEmpty mustBe true
      }
    }

    "have consistent placeholder format" in {
      RiddlFoldingBuilder.FOLDABLE_PATTERNS.foreach { case (_, placeholder) =>
        placeholder must include("{")
        placeholder must include("}")
      }
    }
  }
}
