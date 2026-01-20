/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.completion

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL completion contributor. */
class RiddlCompletionSpec extends AnyWordSpec with Matchers {

  "RiddlKeywords" must {

    "have top-level keywords" in {
      RiddlKeywords.TOP_LEVEL.nonEmpty mustBe true
      RiddlKeywords.TOP_LEVEL must contain("domain")
    }

    "have domain-level keywords" in {
      RiddlKeywords.DOMAIN_LEVEL.nonEmpty mustBe true
      RiddlKeywords.DOMAIN_LEVEL must contain("context")
      RiddlKeywords.DOMAIN_LEVEL must contain("type")
    }

    "have context-level keywords" in {
      RiddlKeywords.CONTEXT_LEVEL.nonEmpty mustBe true
      RiddlKeywords.CONTEXT_LEVEL must contain("entity")
      RiddlKeywords.CONTEXT_LEVEL must contain("repository")
      RiddlKeywords.CONTEXT_LEVEL must contain("saga")
    }

    "have entity-level keywords" in {
      RiddlKeywords.ENTITY_LEVEL.nonEmpty mustBe true
      RiddlKeywords.ENTITY_LEVEL must contain("state")
      RiddlKeywords.ENTITY_LEVEL must contain("handler")
    }

    "have handler-level keywords" in {
      RiddlKeywords.HANDLER_LEVEL.nonEmpty mustBe true
      RiddlKeywords.HANDLER_LEVEL must contain("on")
    }

    "have statement keywords" in {
      RiddlKeywords.STATEMENT_KEYWORDS.nonEmpty mustBe true
      RiddlKeywords.STATEMENT_KEYWORDS must contain("set")
      RiddlKeywords.STATEMENT_KEYWORDS must contain("send")
      RiddlKeywords.STATEMENT_KEYWORDS must contain("tell")
    }

    "have type keywords" in {
      RiddlKeywords.TYPE_KEYWORDS.nonEmpty mustBe true
      RiddlKeywords.TYPE_KEYWORDS must contain("type")
      RiddlKeywords.TYPE_KEYWORDS must contain("record")
      RiddlKeywords.TYPE_KEYWORDS must contain("enumeration")
    }

    "have readability words" in {
      RiddlKeywords.READABILITY_WORDS.nonEmpty mustBe true
      RiddlKeywords.READABILITY_WORDS must contain("is")
      RiddlKeywords.READABILITY_WORDS must contain("of")
      RiddlKeywords.READABILITY_WORDS must contain("for")
    }

    "have predefined types" in {
      RiddlKeywords.PREDEFINED_TYPES.nonEmpty mustBe true
      RiddlKeywords.PREDEFINED_TYPES must contain("String")
      RiddlKeywords.PREDEFINED_TYPES must contain("Integer")
      RiddlKeywords.PREDEFINED_TYPES must contain("Boolean")
      RiddlKeywords.PREDEFINED_TYPES must contain("UUID")
      RiddlKeywords.PREDEFINED_TYPES must contain("DateTime")
    }

    "have all keywords combined" in {
      RiddlKeywords.ALL_KEYWORDS.nonEmpty mustBe true
      // Should contain keywords from all categories
      RiddlKeywords.ALL_KEYWORDS must contain("domain")
      RiddlKeywords.ALL_KEYWORDS must contain("context")
      RiddlKeywords.ALL_KEYWORDS must contain("entity")
      RiddlKeywords.ALL_KEYWORDS must contain("is")
    }

    "have distinct all keywords" in {
      // No duplicates in ALL_KEYWORDS
      RiddlKeywords.ALL_KEYWORDS.distinct.size mustBe RiddlKeywords.ALL_KEYWORDS.size
    }
  }

  "CompletionContext" must {

    "have all required context values" in {
      // Verify enum values exist
      CompletionContext.TopLevel
      CompletionContext.InDomain
      CompletionContext.InContext
      CompletionContext.InEntity
      CompletionContext.InHandler
      CompletionContext.TypePosition
      CompletionContext.AfterIs
      CompletionContext.Unknown
    }
  }

  "RiddlCompletionContributor" must {

    "be instantiable" in {
      val contributor = new RiddlCompletionContributor()
      contributor must not be null
    }
  }

  "RiddlKeywordCompletionProvider" must {

    "be instantiable" in {
      val provider = new RiddlKeywordCompletionProvider()
      provider must not be null
    }
  }

  "Predefined types coverage" must {

    "include basic types" in {
      val basic = Seq("String", "Integer", "Boolean", "Number", "Real")
      basic.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }

    "include temporal types" in {
      val temporal = Seq("Date", "Time", "DateTime", "TimeStamp", "Duration")
      temporal.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }

    "include identifier types" in {
      val identifiers = Seq("UUID", "Id", "UserId")
      identifiers.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }

    "include network types" in {
      val network = Seq("URL", "URI", "Email")
      network.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }

    "include numeric types" in {
      val numeric = Seq("Natural", "Whole", "Real", "Number")
      numeric.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }

    "include location types" in {
      val location = Seq("Location", "LatLong")
      location.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }

    "include unit types" in {
      val units = Seq("Currency", "Length", "Mass", "Temperature")
      units.foreach { t =>
        RiddlKeywords.PREDEFINED_TYPES must contain(t)
      }
    }
  }
}
