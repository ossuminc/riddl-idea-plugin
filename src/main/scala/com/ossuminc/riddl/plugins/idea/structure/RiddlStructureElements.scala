/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.{SortableTreeElement, TreeElement}
import com.intellij.navigation.{ItemPresentation, NavigationItem}
import com.intellij.psi.PsiFile
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.language.At
import com.ossuminc.riddl.utils.{NullLogger, PlatformContext, pc}

import javax.swing.Icon
import scala.collection.mutable.ArrayBuffer

/** Represents a RIDDL definition in the structure view. */
case class RiddlDefinition(
    kind: String,
    name: String,
    offset: Int,
    endOffset: Int,
    children: Seq[RiddlDefinition] = Seq.empty
)

/** Root structure element for a RIDDL file. */
class RiddlFileStructureElement(psiFile: PsiFile)
    extends StructureViewTreeElement
    with SortableTreeElement {

  private lazy val definitions: Seq[RiddlDefinition] = parseDefinitions()

  override def getValue: AnyRef = psiFile

  override def getAlphaSortKey: String = psiFile.getName

  override def getPresentation: ItemPresentation = new ItemPresentation {
    override def getPresentableText: String = psiFile.getName
    override def getIcon(unused: Boolean): Icon = RiddlStructureIcons.FILE
  }

  override def getChildren: Array[TreeElement] =
    definitions.map(d => new RiddlDefinitionElement(d, psiFile): TreeElement).toArray

  override def navigate(requestFocus: Boolean): Unit =
    if psiFile.isInstanceOf[NavigationItem] then
      psiFile.asInstanceOf[NavigationItem].navigate(requestFocus)

  override def canNavigate: Boolean = psiFile.isInstanceOf[NavigationItem]

  override def canNavigateToSource: Boolean = canNavigate

  private def parseDefinitions(): Seq[RiddlDefinition] =
    if psiFile == null || psiFile.getText == null || psiFile.getText.isEmpty then Seq.empty
    else RiddlStructureParser.parseDefinitions(psiFile.getText)
}

/** Structure element for a RIDDL definition (domain, context, entity, etc.). */
class RiddlDefinitionElement(definition: RiddlDefinition, psiFile: PsiFile)
    extends StructureViewTreeElement
    with SortableTreeElement {

  override def getValue: AnyRef = definition

  override def getAlphaSortKey: String = definition.name

  override def getPresentation: ItemPresentation = new ItemPresentation {
    override def getPresentableText: String = definition.name

    override def getLocationString: String = definition.kind

    override def getIcon(unused: Boolean): Icon =
      RiddlStructureIcons.forKind(definition.kind)
  }

  override def getChildren: Array[TreeElement] =
    definition.children.map(d => new RiddlDefinitionElement(d, psiFile): TreeElement).toArray

  def hasChildren: Boolean = definition.children.nonEmpty

  override def navigate(requestFocus: Boolean): Unit =
    if psiFile.isInstanceOf[NavigationItem] then
      // Navigate to the definition's location in the file
      val editor = com.intellij.openapi.fileEditor.FileEditorManager
        .getInstance(psiFile.getProject)
        .getSelectedTextEditor
      if editor != null then editor.getCaretModel.moveToOffset(definition.offset)

  override def canNavigate: Boolean = true

  override def canNavigateToSource: Boolean = true
}

/** Parser for extracting RIDDL definitions from source text. */
object RiddlStructureParser {

  /** Regex patterns for extracting definitions.
    * Each pattern captures: optional description, keyword, name
    */
  private val DEFINITION_PATTERNS: Seq[(String, scala.util.matching.Regex)] = Seq(
    ("domain", """(?m)^\s*(domain)\s+(\w+)""".r),
    ("context", """(?m)^\s*(context)\s+(\w+)""".r),
    ("entity", """(?m)^\s*(entity)\s+(\w+)""".r),
    ("adaptor", """(?m)^\s*(adaptor)\s+(\w+)""".r),
    ("application", """(?m)^\s*(application)\s+(\w+)""".r),
    ("epic", """(?m)^\s*(epic)\s+(\w+)""".r),
    ("saga", """(?m)^\s*(saga)\s+(\w+)""".r),
    ("repository", """(?m)^\s*(repository)\s+(\w+)""".r),
    ("projector", """(?m)^\s*(projector)\s+(\w+)""".r),
    ("streamlet", """(?m)^\s*(streamlet)\s+(\w+)""".r),
    ("connector", """(?m)^\s*(connector)\s+(\w+)""".r),
    ("handler", """(?m)^\s*(handler)\s+(\w+)""".r),
    ("function", """(?m)^\s*(function)\s+(\w+)""".r),
    ("state", """(?m)^\s*(state)\s+(\w+)""".r),
    ("type", """(?m)^\s*(type)\s+(\w+)""".r),
    ("constant", """(?m)^\s*(constant)\s+(\w+)""".r),
    ("inlet", """(?m)^\s*(inlet)\s+(\w+)""".r),
    ("outlet", """(?m)^\s*(outlet)\s+(\w+)""".r),
    ("command", """(?m)^\s*(command)\s+(\w+)""".r),
    ("event", """(?m)^\s*(event)\s+(\w+)""".r),
    ("query", """(?m)^\s*(query)\s+(\w+)""".r),
    ("result", """(?m)^\s*(result)\s+(\w+)""".r),
    ("record", """(?m)^\s*(record)\s+(\w+)""".r)
  )

  /** Parse definitions from RIDDL source text.
    *
    * This uses a simplified regex-based approach for structure view.
    * It extracts top-level and nested definitions without full parsing.
    */
  def parseDefinitions(text: String): Seq[RiddlDefinition] = {
    val allMatches = ArrayBuffer[(String, String, Int, Int)]()

    // Find all definition matches
    DEFINITION_PATTERNS.foreach { case (kind, pattern) =>
      pattern.findAllMatchIn(text).foreach { m =>
        val name = m.group(2)
        val start = m.start
        val end = m.end
        allMatches += ((kind, name, start, end))
      }
    }

    // Sort by position
    val sorted = allMatches.sortBy(_._3).toSeq

    // Build hierarchy based on brace nesting
    buildHierarchy(text, sorted)
  }

  /** Build a hierarchical structure based on brace nesting levels. */
  private def buildHierarchy(
      text: String,
      definitions: Seq[(String, String, Int, Int)]
  ): Seq[RiddlDefinition] = {
    if definitions.isEmpty then return Seq.empty

    // Calculate nesting level at each definition's position
    def nestingLevel(position: Int): Int = {
      var level = 0
      var i = 0
      while i < position && i < text.length do {
        if text.charAt(i) == '{' then level += 1
        else if text.charAt(i) == '}' then level -= 1
        i += 1
      }
      level
    }

    // Find the end of a definition (matching closing brace)
    def findDefinitionEnd(startPos: Int): Int = {
      var level = 0
      var foundOpenBrace = false
      var i = startPos
      while i < text.length do {
        val c = text.charAt(i)
        if c == '{' then {
          level += 1
          foundOpenBrace = true
        } else if c == '}' then {
          level -= 1
          if foundOpenBrace && level == 0 then return i + 1
        }
        i += 1
      }
      text.length
    }

    // Build tree structure
    val withLevels = definitions.map { case (kind, name, start, end) =>
      (kind, name, start, findDefinitionEnd(start), nestingLevel(start))
    }

    // Get top-level definitions (level 0)
    val topLevel = withLevels.filter(_._5 == 0)

    topLevel.map { case (kind, name, start, end, _) =>
      val children = withLevels
        .filter { case (_, _, childStart, childEnd, level) =>
          level > 0 && childStart > start && childEnd <= end
        }
        .filter(_._5 == 1) // Only direct children (level 1)
        .map { case (ck, cn, cs, ce, _) =>
          RiddlDefinition(ck, cn, cs, ce)
        }
      RiddlDefinition(kind, name, start, end, children)
    }
  }
}
