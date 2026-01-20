/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.editor

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Tests for RIDDL editor components (brace matcher and commenter).
  *
  * These tests verify basic functionality.
  * Full integration testing requires IntelliJ platform test fixtures.
  */
class RiddlEditorSpec extends AnyWordSpec with Matchers {

  "RiddlBraceMatcher" must {

    "have brace pairs defined" in {
      val matcher = new RiddlBraceMatcher()
      matcher.getPairs.nonEmpty mustBe true
    }

    "allow paired braces before any type" in {
      val matcher = new RiddlBraceMatcher()
      matcher.isPairedBracesAllowedBeforeType(null, null) mustBe true
    }

    "return opening brace offset as code construct start" in {
      val matcher = new RiddlBraceMatcher()
      matcher.getCodeConstructStart(null, 42) mustBe 42
    }
  }

  "RiddlBraceMatcher companion object" must {

    "recognize brace characters" in {
      RiddlBraceMatcher.BRACE_CHARS must contain('{')
      RiddlBraceMatcher.BRACE_CHARS must contain('}')
      RiddlBraceMatcher.BRACE_CHARS must contain('(')
      RiddlBraceMatcher.BRACE_CHARS must contain(')')
      RiddlBraceMatcher.BRACE_CHARS must contain('[')
      RiddlBraceMatcher.BRACE_CHARS must contain(']')
      RiddlBraceMatcher.BRACE_CHARS must contain('<')
      RiddlBraceMatcher.BRACE_CHARS must contain('>')
    }

    "have opening braces" in {
      RiddlBraceMatcher.OPENING_BRACES must contain('{')
      RiddlBraceMatcher.OPENING_BRACES must contain('(')
      RiddlBraceMatcher.OPENING_BRACES must contain('[')
      RiddlBraceMatcher.OPENING_BRACES must contain('<')
    }

    "have closing braces" in {
      RiddlBraceMatcher.CLOSING_BRACES must contain('}')
      RiddlBraceMatcher.CLOSING_BRACES must contain(')')
      RiddlBraceMatcher.CLOSING_BRACES must contain(']')
      RiddlBraceMatcher.CLOSING_BRACES must contain('>')
    }

    "not have opening braces in closing set" in {
      RiddlBraceMatcher.CLOSING_BRACES must not contain '{'
      RiddlBraceMatcher.CLOSING_BRACES must not contain '('
      RiddlBraceMatcher.CLOSING_BRACES must not contain '['
      RiddlBraceMatcher.CLOSING_BRACES must not contain '<'
    }

    "not have closing braces in opening set" in {
      RiddlBraceMatcher.OPENING_BRACES must not contain '}'
      RiddlBraceMatcher.OPENING_BRACES must not contain ')'
      RiddlBraceMatcher.OPENING_BRACES must not contain ']'
      RiddlBraceMatcher.OPENING_BRACES must not contain '>'
    }

    "get matching brace for curly braces" in {
      RiddlBraceMatcher.getMatchingBrace('{') mustBe Some('}')
      RiddlBraceMatcher.getMatchingBrace('}') mustBe Some('{')
    }

    "get matching brace for parentheses" in {
      RiddlBraceMatcher.getMatchingBrace('(') mustBe Some(')')
      RiddlBraceMatcher.getMatchingBrace(')') mustBe Some('(')
    }

    "get matching brace for square brackets" in {
      RiddlBraceMatcher.getMatchingBrace('[') mustBe Some(']')
      RiddlBraceMatcher.getMatchingBrace(']') mustBe Some('[')
    }

    "get matching brace for angle brackets" in {
      RiddlBraceMatcher.getMatchingBrace('<') mustBe Some('>')
      RiddlBraceMatcher.getMatchingBrace('>') mustBe Some('<')
    }

    "return None for non-brace characters" in {
      RiddlBraceMatcher.getMatchingBrace('a') mustBe None
      RiddlBraceMatcher.getMatchingBrace('1') mustBe None
      RiddlBraceMatcher.getMatchingBrace(' ') mustBe None
    }
  }

  "RiddlCommenter" must {

    "have line comment prefix" in {
      val commenter = new RiddlCommenter()
      commenter.getLineCommentPrefix mustBe "// "
    }

    "have block comment prefix" in {
      val commenter = new RiddlCommenter()
      commenter.getBlockCommentPrefix mustBe "/* "
    }

    "have block comment suffix" in {
      val commenter = new RiddlCommenter()
      commenter.getBlockCommentSuffix mustBe " */"
    }

    "not support commented block comments" in {
      val commenter = new RiddlCommenter()
      commenter.getCommentedBlockCommentPrefix mustBe null
      commenter.getCommentedBlockCommentSuffix mustBe null
    }

    "have consistent comment style" in {
      val commenter = new RiddlCommenter()

      // Line comment should start with //
      commenter.getLineCommentPrefix must startWith("//")

      // Block comment should be /* ... */
      commenter.getBlockCommentPrefix must startWith("/*")
      commenter.getBlockCommentSuffix must endWith("*/")
    }
  }
}
