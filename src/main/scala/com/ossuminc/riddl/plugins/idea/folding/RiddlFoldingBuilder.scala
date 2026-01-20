/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.{FoldingBuilderEx, FoldingDescriptor}
import com.intellij.openapi.editor.{Document, FoldingGroup}
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

/** Folding builder for RIDDL files.
  *
  * Provides code folding for RIDDL definitions like domain, context, entity,
  * handler, type, etc. Users can collapse these to see the overall structure.
  */
class RiddlFoldingBuilder extends FoldingBuilderEx {

  /** Build fold regions for the given element. */
  override def buildFoldRegions(
      root: PsiElement,
      document: Document,
      quick: Boolean
  ): Array[FoldingDescriptor] =
    val descriptors = ArrayBuffer.empty[FoldingDescriptor]
    val text = document.getText

    // Find all foldable regions using regex patterns
    RiddlFoldingBuilder.FOLDABLE_PATTERNS.foreach { case (pattern, placeholder) =>
      val matcher = pattern.findAllMatchIn(text)
      matcher.foreach { m =>
        val startOffset = m.start
        val keywordEnd = m.end

        // Find the matching closing brace
        findMatchingBrace(text, keywordEnd) match
          case Some(endOffset) if endOffset > keywordEnd =>
            val range = TextRange.create(keywordEnd, endOffset + 1)
            if range.getLength > 1 then
              val group = FoldingGroup.newGroup(s"riddl-${m.group(1)}")
              descriptors += new FoldingDescriptor(
                root.getNode,
                range,
                group,
                placeholder
              )
          case _ => // No matching brace found
      }
    }

    // Find comment blocks
    findCommentBlocks(text).foreach { range =>
      if range.getLength > 3 then
        descriptors += new FoldingDescriptor(
          root.getNode,
          range,
          FoldingGroup.newGroup("riddl-comment"),
          "/*...*/"
        )
    }

    descriptors.toArray

  /** Find the matching closing brace for an opening brace. */
  private def findMatchingBrace(text: String, startIdx: Int): Option[Int] =
    // Skip to the opening brace
    var idx = startIdx
    while idx < text.length && text.charAt(idx) != '{' do idx += 1

    if idx >= text.length then return None

    var depth = 0
    var i = idx
    while i < text.length do
      text.charAt(i) match
        case '{' => depth += 1
        case '}' =>
          depth -= 1
          if depth == 0 then return Some(i)
        case _ =>
      i += 1
    None

  /** Find multi-line comment blocks. */
  private def findCommentBlocks(text: String): Seq[TextRange] =
    val blocks = ArrayBuffer.empty[TextRange]
    val pattern = """/\*[\s\S]*?\*/""".r

    pattern.findAllMatchIn(text).foreach { m =>
      if m.matched.contains('\n') then
        blocks += TextRange.create(m.start, m.end)
    }
    blocks.toSeq

  /** Get the placeholder text for a folded region. */
  override def getPlaceholderText(node: ASTNode): String = "..."

  /** Whether the fold region should be collapsed by default. */
  override def isCollapsedByDefault(node: ASTNode): Boolean = false
}

object RiddlFoldingBuilder {

  /** Patterns for foldable RIDDL definitions.
    *
    * Each tuple contains:
    * - Regex pattern to match the definition start
    * - Placeholder text to show when folded
    */
  val FOLDABLE_PATTERNS: Seq[(Regex, String)] = Seq(
    ("""(?m)^\s*(domain)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(context)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(entity)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(adaptor)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(repository)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(projector)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(saga)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(streamlet)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(handler)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(function)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(state)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(type)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(epic)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(case)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(application)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(group)\s+(\w+)""".r, " {...}"),
    ("""(?m)^\s*(on)\s+(command|event|query|other)""".r, " {...}")
  )
}
