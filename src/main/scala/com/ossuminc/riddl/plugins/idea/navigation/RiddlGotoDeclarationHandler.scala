/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.{PsiElement, PsiFile}
import com.ossuminc.riddl.RiddlLib
import com.ossuminc.riddl.utils.{NullLogger, pc}

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

/** Handler for go-to-declaration (Cmd+Click or Cmd+B) in RIDDL files.
  *
  * Finds the definition of a referenced type or identifier and navigates to it.
  */
class RiddlGotoDeclarationHandler extends GotoDeclarationHandler {

  override def getGotoDeclarationTargets(
      sourceElement: PsiElement,
      offset: Int,
      editor: Editor
  ): Array[PsiElement] = {
    if sourceElement == null then return null

    val file = sourceElement.getContainingFile
    if file == null then return null

    val text = file.getText
    if text == null || text.isEmpty then return null

    // Get the identifier at the current position
    val identifier = getIdentifierAtOffset(text, offset)
    if identifier == null || identifier.isEmpty then return null

    // Find the definition of this identifier
    val definitions = findDefinitions(text, identifier)

    if definitions.isEmpty then null
    else {
      // Create navigatable elements for each definition
      definitions.flatMap { case (defOffset, _) =>
        createNavigatableElement(file, defOffset)
      }.toArray
    }
  }

  override def getActionText(context: DataContext): String = "Go to RIDDL Definition"

  /** Extract the identifier at the given offset. */
  private def getIdentifierAtOffset(text: String, offset: Int): String = {
    if offset < 0 || offset >= text.length then return null

    // Find word boundaries
    var start = offset
    var end = offset

    // Move back to start of word
    while start > 0 && isIdentifierChar(text.charAt(start - 1)) do start -= 1

    // Move forward to end of word
    while end < text.length && isIdentifierChar(text.charAt(end)) do end += 1

    if start == end then null
    else text.substring(start, end)
  }

  private def isIdentifierChar(c: Char): Boolean =
    c.isLetterOrDigit || c == '_'

  /** Find all definitions of the given identifier.
    *
    * Tries RiddlLib.getOutline() first for AST-accurate results.
    * Falls back to regex for fragment files that can't parse as Root.
    */
  private def findDefinitions(text: String, identifier: String): Seq[(Int, String)] = {
    pc.withLogger(NullLogger()) { _ =>
      RiddlLib.getOutline(text, "navigation")(using pc) match
        case Right(entries) =>
          entries
            .filter(_.id == identifier)
            .map(e => (e.offset, e.kind.toLowerCase))
        case Left(_) =>
          findDefinitionsRegex(text, identifier)
    }
  }

  /** Regex-based fallback for finding definitions in fragment files. */
  private def findDefinitionsRegex(text: String, identifier: String): Seq[(Int, String)] = {
    val results = ArrayBuffer[(Int, String)]()

    val patterns: Seq[(String, Regex)] = Seq(
      ("domain", s"""(?m)^\\s*(domain)\\s+($identifier)\\s""".r),
      ("context", s"""(?m)^\\s*(context)\\s+($identifier)\\s""".r),
      ("entity", s"""(?m)^\\s*(entity)\\s+($identifier)\\s""".r),
      ("type", s"""(?m)^\\s*(type)\\s+($identifier)\\s""".r),
      ("handler", s"""(?m)^\\s*(handler)\\s+($identifier)\\s""".r),
      ("state", s"""(?m)^\\s*(state)\\s+($identifier)\\s""".r),
      ("function", s"""(?m)^\\s*(function)\\s+($identifier)\\s""".r),
      ("repository", s"""(?m)^\\s*(repository)\\s+($identifier)\\s""".r),
      ("saga", s"""(?m)^\\s*(saga)\\s+($identifier)\\s""".r),
      ("projector", s"""(?m)^\\s*(projector)\\s+($identifier)\\s""".r),
      ("streamlet", s"""(?m)^\\s*(streamlet)\\s+($identifier)\\s""".r),
      ("connector", s"""(?m)^\\s*(connector)\\s+($identifier)\\s""".r),
      ("adaptor", s"""(?m)^\\s*(adaptor)\\s+($identifier)\\s""".r),
      ("application", s"""(?m)^\\s*(application)\\s+($identifier)\\s""".r),
      ("epic", s"""(?m)^\\s*(epic)\\s+($identifier)\\s""".r),
      ("constant", s"""(?m)^\\s*(constant)\\s+($identifier)\\s""".r),
      ("command", s"""(?m)^\\s*(command)\\s+($identifier)\\s""".r),
      ("event", s"""(?m)^\\s*(event)\\s+($identifier)\\s""".r),
      ("query", s"""(?m)^\\s*(query)\\s+($identifier)\\s""".r),
      ("result", s"""(?m)^\\s*(result)\\s+($identifier)\\s""".r),
      ("record", s"""(?m)^\\s*(record)\\s+($identifier)\\s""".r),
      ("inlet", s"""(?m)^\\s*(inlet)\\s+($identifier)\\s""".r),
      ("outlet", s"""(?m)^\\s*(outlet)\\s+($identifier)\\s""".r)
    )

    patterns.foreach { case (kind, pattern) =>
      pattern.findAllMatchIn(text).foreach { m =>
        results += ((m.start(2), kind))
      }
    }

    results.toSeq
  }

  /** Create a navigatable PsiElement at the given offset. */
  private def createNavigatableElement(file: PsiFile, offset: Int): Option[PsiElement] = {
    val element = file.findElementAt(offset)
    if element != null then Some(element)
    else None
  }
}

/** Utilities for RIDDL navigation. */
object RiddlNavigationUtils {

  /** Find all references to a given identifier in the text. */
  def findReferences(text: String, identifier: String): Seq[Int] = {
    val results = ArrayBuffer[Int]()
    val pattern = s"""\\b$identifier\\b""".r

    pattern.findAllMatchIn(text).foreach { m =>
      results += m.start
    }

    results.toSeq
  }

  /** Check if an offset is within a definition (not a reference). */
  def isDefinitionPosition(text: String, offset: Int): Boolean = {
    // Look backwards for a keyword
    val before = text.substring(0, math.min(offset, text.length))
    val lastLine = before.split("\n").lastOption.getOrElse("")

    val definitionKeywords = Set(
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

    definitionKeywords.exists(kw => lastLine.trim.startsWith(kw))
  }
}
