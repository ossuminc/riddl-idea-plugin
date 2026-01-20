/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder}
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.ossuminc.riddl.plugins.idea.RiddlLanguage

import javax.swing.Icon

/** Completion contributor for RIDDL language.
  *
  * Provides code completion for:
  * - RIDDL keywords (domain, context, entity, etc.)
  * - Predefined types (String, Integer, Boolean, etc.)
  * - Readability words (is, of, for, etc.)
  */
class RiddlCompletionContributor extends CompletionContributor {

  // Register completion providers
  extend(
    CompletionType.BASIC,
    PlatformPatterns.psiElement(),
    new RiddlKeywordCompletionProvider
  )
}

/** Provides keyword completion for RIDDL. */
class RiddlKeywordCompletionProvider extends CompletionProvider[CompletionParameters] {

  override def addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet
  ): Unit = {
    val position = parameters.getPosition
    val text = parameters.getOriginalFile.getText
    val offset = parameters.getOffset

    // Determine completion context
    val completionContext = determineContext(text, offset)

    completionContext match {
      case CompletionContext.TopLevel =>
        addTopLevelKeywords(result)
      case CompletionContext.InDomain =>
        addDomainKeywords(result)
      case CompletionContext.InContext =>
        addContextKeywords(result)
      case CompletionContext.InEntity =>
        addEntityKeywords(result)
      case CompletionContext.InHandler =>
        addHandlerKeywords(result)
      case CompletionContext.TypePosition =>
        addTypeKeywords(result)
        addPredefinedTypes(result)
      case CompletionContext.AfterIs =>
        addAfterIsKeywords(result)
        addPredefinedTypes(result)
      case CompletionContext.Unknown =>
        // Add all keywords as fallback
        addAllKeywords(result)
    }
  }

  private def determineContext(text: String, offset: Int): CompletionContext = {
    val before = text.substring(0, math.min(offset, text.length))
    val lines = before.split("\n")
    val currentLine = if lines.nonEmpty then lines.last else ""

    // Check if we're after "is"
    if currentLine.trim.endsWith("is") || before.trim.endsWith("is ") then
      return CompletionContext.AfterIs

    // Check if we're in a type position (after ":")
    if currentLine.contains(":") && !currentLine.contains("{") then
      return CompletionContext.TypePosition

    // Count braces to determine nesting
    var domainLevel = 0
    var contextLevel = 0
    var entityLevel = 0
    var handlerLevel = 0

    val patterns = List(
      ("domain", () => domainLevel += 1),
      ("context", () => contextLevel += 1),
      ("entity", () => entityLevel += 1),
      ("handler", () => handlerLevel += 1)
    )

    // Simple heuristic based on what's open
    if before.contains("handler") && before.lastIndexOf("handler") > before.lastIndexOf("}") then
      CompletionContext.InHandler
    else if before.contains("entity") && before.lastIndexOf("entity") > before.lastIndexOf("}") then
      CompletionContext.InEntity
    else if before.contains("context") && before.lastIndexOf("context") > before.lastIndexOf("}") then
      CompletionContext.InContext
    else if before.contains("domain") && before.lastIndexOf("domain") > before.lastIndexOf("}") then
      CompletionContext.InDomain
    else CompletionContext.TopLevel
  }

  private def addTopLevelKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.TOP_LEVEL.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addDomainKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.DOMAIN_LEVEL.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addContextKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.CONTEXT_LEVEL.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addEntityKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.ENTITY_LEVEL.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addHandlerKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.HANDLER_LEVEL.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addTypeKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.TYPE_KEYWORDS.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addAfterIsKeywords(result: CompletionResultSet): Unit = {
    result.addElement(createKeywordElement("{"))
    RiddlKeywords.TYPE_KEYWORDS.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
  }

  private def addPredefinedTypes(result: CompletionResultSet): Unit = {
    RiddlKeywords.PREDEFINED_TYPES.foreach { t =>
      result.addElement(createTypeElement(t))
    }
  }

  private def addAllKeywords(result: CompletionResultSet): Unit = {
    RiddlKeywords.ALL_KEYWORDS.foreach { kw =>
      result.addElement(createKeywordElement(kw))
    }
    RiddlKeywords.PREDEFINED_TYPES.foreach { t =>
      result.addElement(createTypeElement(t))
    }
  }

  private def createKeywordElement(keyword: String): LookupElement =
    LookupElementBuilder
      .create(keyword)
      .withBoldness(true)
      .withTypeText("keyword")

  private def createTypeElement(typeName: String): LookupElement =
    LookupElementBuilder
      .create(typeName)
      .withTypeText("predefined type")
      .withItemTextItalic(true)
}

/** Completion context enum. */
enum CompletionContext {
  case TopLevel
  case InDomain
  case InContext
  case InEntity
  case InHandler
  case TypePosition
  case AfterIs
  case Unknown
}

/** RIDDL keywords organized by context. */
object RiddlKeywords {

  val TOP_LEVEL: Seq[String] = Seq(
    "domain",
    "author",
    "include"
  )

  val DOMAIN_LEVEL: Seq[String] = Seq(
    "context",
    "type",
    "constant",
    "author",
    "include",
    "term"
  )

  val CONTEXT_LEVEL: Seq[String] = Seq(
    "entity",
    "adaptor",
    "application",
    "epic",
    "saga",
    "repository",
    "projector",
    "streamlet",
    "connector",
    "type",
    "constant",
    "function",
    "term"
  )

  val ENTITY_LEVEL: Seq[String] = Seq(
    "state",
    "handler",
    "function",
    "type",
    "constant",
    "invariant"
  )

  val HANDLER_LEVEL: Seq[String] = Seq(
    "on",
    "command",
    "event",
    "query"
  )

  val STATEMENT_KEYWORDS: Seq[String] = Seq(
    "set",
    "send",
    "tell",
    "call",
    "become",
    "morph",
    "return",
    "error",
    "if",
    "then",
    "else",
    "for",
    "foreach",
    "while",
    "do"
  )

  val TYPE_KEYWORDS: Seq[String] = Seq(
    "type",
    "record",
    "mapping",
    "range",
    "enumeration",
    "aggregation",
    "alternation",
    "one",
    "many",
    "optional",
    "sequence"
  )

  val READABILITY_WORDS: Seq[String] = Seq(
    "is",
    "are",
    "of",
    "to",
    "from",
    "by",
    "for",
    "with",
    "as",
    "in",
    "on",
    "at"
  )

  val PREDEFINED_TYPES: Seq[String] = Seq(
    "String",
    "Integer",
    "Natural",
    "Whole",
    "Real",
    "Number",
    "Boolean",
    "Id",
    "Date",
    "Time",
    "DateTime",
    "TimeStamp",
    "Duration",
    "UUID",
    "URL",
    "URI",
    "Email",
    "PhoneNumber",
    "UserId",
    "Location",
    "LatLong",
    "Nothing",
    "Abstract",
    "Currency",
    "Length",
    "Mass",
    "Temperature",
    "Luminosity",
    "Current",
    "Mole"
  )

  val ALL_KEYWORDS: Seq[String] =
    (TOP_LEVEL ++ DOMAIN_LEVEL ++ CONTEXT_LEVEL ++ ENTITY_LEVEL ++
      HANDLER_LEVEL ++ STATEMENT_KEYWORDS ++ TYPE_KEYWORDS ++ READABILITY_WORDS).distinct
}
