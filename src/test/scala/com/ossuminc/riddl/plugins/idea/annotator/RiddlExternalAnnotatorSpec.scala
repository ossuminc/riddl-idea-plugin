/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.annotator

import com.ossuminc.riddl.language.Messages.{Error, SevereError}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL external annotator.
  *
  * These tests verify annotation collection and result processing.
  * Full integration testing requires IntelliJ platform test fixtures.
  */
class RiddlExternalAnnotatorSpec extends AnyWordSpec with Matchers {

  "RiddlExternalAnnotator" must {

    "collect annotation info from text" in {
      val annotator = new RiddlExternalAnnotator()
      val text = "domain Test is { }"
      val filePath = "/test/example.riddl"

      val info = RiddlAnnotationInfo(text, filePath)

      info.text mustBe text
      info.filePath mustBe filePath
    }

    "return no errors for valid RIDDL" in {
      val annotator = new RiddlExternalAnnotator()
      // RIDDL requires at least a ???  placeholder or actual content
      val info = RiddlAnnotationInfo("domain Test is { ??? }", "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      // Full validation may produce MissingWarning/StyleWarning for
      // minimal models, but should have no Error-level messages
      val errors = result.messages.filter(m =>
        m.kind == Error || m.kind == SevereError
      )
      errors mustBe empty
    }

    "return empty result for empty text" in {
      val annotator = new RiddlExternalAnnotator()
      val info = RiddlAnnotationInfo("", "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      result.messages mustBe empty
    }

    "detect syntax errors in invalid RIDDL" in {
      val annotator = new RiddlExternalAnnotator()
      val info = RiddlAnnotationInfo("domain { incomplete", "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      result.messages.nonEmpty mustBe true
    }

    "detect missing closing brace" in {
      val annotator = new RiddlExternalAnnotator()
      val info = RiddlAnnotationInfo("domain Test is {", "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      result.messages.nonEmpty mustBe true
    }

    "handle complex valid RIDDL without errors" in {
      val annotator = new RiddlExternalAnnotator()
      // Use a simpler but valid RIDDL structure
      val text =
        """domain Example is {
          |  context UserContext is {
          |    type UserId is String
          |  }
          |}""".stripMargin
      val info = RiddlAnnotationInfo(text, "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      // Full validation may produce warnings for minimal models
      // (missing descriptions, empty metadata, unused types) but
      // should have no Error-level messages
      val errors = result.messages.filter(m =>
        m.kind == Error || m.kind == SevereError
      )
      errors mustBe empty
    }

    "detect semantic errors via full validation" in {
      val annotator = new RiddlExternalAnnotator()
      val text =
        """domain Test is {
          |  context C is {
          |    type Ref is reference to UndefinedEntity
          |  }
          |}""".stripMargin
      val info = RiddlAnnotationInfo(text, "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      // Full validation should find unresolvable reference
      result.messages.nonEmpty mustBe true
    }

    "handle RIDDL fragments via nebula fallback" in {
      val annotator = new RiddlExternalAnnotator()
      // This is a fragment (not a complete Root document)
      val text =
        """context TestContext is {
          |  type UserId is String
          |}""".stripMargin
      val info = RiddlAnnotationInfo(text, "/test/fragment.riddl")

      val result = annotator.doAnnotate(info)

      // Fragment should parse OK via nebula fallback
      result.messages mustBe empty
    }
  }

  "RiddlAnnotationInfo" must {

    "store text and file path" in {
      val info = RiddlAnnotationInfo("test content", "/path/to/file.riddl")

      info.text mustBe "test content"
      info.filePath mustBe "/path/to/file.riddl"
    }
  }

  "RiddlAnnotationResult" must {

    "store messages" in {
      val result = RiddlAnnotationResult(Seq.empty)

      result.messages mustBe empty
    }
  }
}
