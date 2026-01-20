/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.editor

import com.intellij.lang.{BracePair, PairedBraceMatcher}
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.ossuminc.riddl.plugins.idea.lexer.RiddlTokenTypes

/** Brace matcher for RIDDL files.
  *
  * Provides brace matching and highlighting for:
  * - Curly braces: { }
  * - Parentheses: ( )
  * - Square brackets: [ ]
  * - Angle brackets: < >
  */
class RiddlBraceMatcher extends PairedBraceMatcher {

  override def getPairs: Array[BracePair] = RiddlBraceMatcher.PAIRS

  override def isPairedBracesAllowedBeforeType(
      lbraceType: IElementType,
      contextType: IElementType
  ): Boolean = true

  override def getCodeConstructStart(
      file: PsiFile,
      openingBraceOffset: Int
  ): Int = openingBraceOffset
}

object RiddlBraceMatcher {

  /** Brace pairs for RIDDL.
    *
    * Note: Since we use a single PUNCTUATION token type for all punctuation,
    * IntelliJ's default brace matching will work. This class is provided for
    * future expansion if we add more specific token types.
    *
    * The structural flag indicates whether this brace pair defines a code block
    * (like { } for definitions) vs a non-structural pair (like < > for generics).
    */
  val PAIRS: Array[BracePair] = Array(
    // Structural braces (code blocks)
    new BracePair(
      RiddlTokenTypes.PUNCTUATION, // {
      RiddlTokenTypes.PUNCTUATION, // }
      true                         // structural
    )
  )

  /** Characters that RIDDL considers as braces. */
  val BRACE_CHARS: Set[Char] = Set('{', '}', '(', ')', '[', ']', '<', '>')

  /** Opening brace characters. */
  val OPENING_BRACES: Set[Char] = Set('{', '(', '[', '<')

  /** Closing brace characters. */
  val CLOSING_BRACES: Set[Char] = Set('}', ')', ']', '>')

  /** Get the matching brace for a given character. */
  def getMatchingBrace(c: Char): Option[Char] = c match
    case '{' => Some('}')
    case '}' => Some('{')
    case '(' => Some(')')
    case ')' => Some('(')
    case '[' => Some(']')
    case ']' => Some('[')
    case '<' => Some('>')
    case '>' => Some('<')
    case _   => None
}
