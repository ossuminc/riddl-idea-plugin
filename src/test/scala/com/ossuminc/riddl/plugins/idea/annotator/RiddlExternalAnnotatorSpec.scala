/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.ossuminc.riddl.language.At
import com.ossuminc.riddl.language.Messages.{
  CompletenessWarning,
  Deprecation,
  Error,
  Info,
  KindOfMessage,
  Message,
  MissingWarning,
  SevereError,
  StyleWarning,
  Tip,
  UsageWarning,
  Warning
}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for the RIDDL external annotator.
  *
  * These tests verify annotation collection and result processing. Full integration testing
  * requires IntelliJ platform test fixtures.
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
      val errors = result.messages.filter(m => m.kind == Error || m.kind == SevereError)
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
      // Use a simpler but valid RIDDL structure. Avoid naming the type
      // UserId: riddl 2.0 made UserId a predefined type, so redefining it
      // is an Error ("redefines built-in type") and the model would no
      // longer be the valid one this test intends to exercise.
      val text =
        """domain Example is {
          |  context UserContext is {
          |    type CustomerId is String
          |  }
          |}""".stripMargin
      val info = RiddlAnnotationInfo(text, "/test/example.riddl")

      val result = annotator.doAnnotate(info)

      // Full validation may produce warnings for minimal models
      // (missing descriptions, empty metadata, unused types) but
      // should have no Error-level messages
      val errors = result.messages.filter(m => m.kind == Error || m.kind == SevereError)
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

  "RiddlExternalAnnotator severity mapping" must {

    // riddl 2.0 added Tip, Deprecation and CompletenessWarning to
    // KindOfMessage. The compiler's exhaustivity check under -Werror is what
    // guarantees every kind is HANDLED; these cases pin WHICH severity each
    // one is handled AS, which is a judgement call the compiler cannot make.
    val expected: Seq[(KindOfMessage, HighlightSeverity)] = Seq(
      SevereError -> HighlightSeverity.ERROR,
      Error -> HighlightSeverity.ERROR,
      Warning -> HighlightSeverity.WARNING,
      CompletenessWarning -> HighlightSeverity.WARNING,
      MissingWarning -> HighlightSeverity.WARNING,
      UsageWarning -> HighlightSeverity.WEAK_WARNING,
      Deprecation -> HighlightSeverity.WEAK_WARNING,
      StyleWarning -> HighlightSeverity.WEAK_WARNING,
      Info -> HighlightSeverity.INFORMATION,
      Tip -> HighlightSeverity.INFORMATION
    )

    expected.foreach { case (kind, severity) =>
      s"map $kind to $severity" in {
        val annotator = new RiddlExternalAnnotator()
        val msg = Message(At.empty, s"a $kind message", kind)

        annotator.mapSeverity(msg) mustBe severity
      }
    }

    "treat an error as at least as severe as any warning" in {
      val annotator = new RiddlExternalAnnotator()
      val errorSeverity =
        annotator.mapSeverity(Message(At.empty, "boom", Error))
      val warningSeverity =
        annotator.mapSeverity(Message(At.empty, "meh", Warning))

      errorSeverity.compareTo(warningSeverity) must be > 0
    }

    "escalate CompletenessWarning above the advisory warning kinds" in {
      // CompletenessWarning is riddl's isActionable threshold, so it must not
      // be demoted to the weak treatment given Deprecation/Style/Usage.
      val annotator = new RiddlExternalAnnotator()
      val completeness =
        annotator.mapSeverity(Message(At.empty, "incomplete", CompletenessWarning))
      val deprecation =
        annotator.mapSeverity(Message(At.empty, "old", Deprecation))

      completeness.compareTo(deprecation) must be > 0
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
