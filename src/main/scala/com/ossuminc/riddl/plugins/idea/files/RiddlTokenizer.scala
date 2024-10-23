package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.ossuminc.riddl.plugins.idea.files.utils.splitByBlanks

object RiddlTokenizer {
  // Outputs:
  // String - word
  // Int - index
  // Boolean - isQuoted, isComment
  def tokenize(text: String): Seq[(String, Int, Seq[Boolean])] = {
    var currentIndex = 0
    tokenizeWithFlags(text).map { case (word, Seq(isQuoted, isComment)) =>
      val tuple = (word, currentIndex, Seq(isQuoted, isComment))
      currentIndex += word.length
      tuple
    }
  }

  private def tokenizeWithFlags(text: String): Seq[(String, Seq[Boolean])] = {
    var inQuotes: Boolean = false
    var inComment: Boolean = false

    def checkForComment(word: String): Unit =
      if word.startsWith("//") && !inComment then inComment = true
      else if word.contains('\n') && inComment then inComment = false

    def checkForQuotes(word: String): Boolean =
      if word.count(c => c == '\"') == 2 then true
      else if word.startsWith("\"") then
        inQuotes = true
        inQuotes
      else if word.endsWith("\"") then
        inQuotes = false
        true
      else inQuotes

    splitByBlanks(text).toSeq.foldLeft(List[(String, Seq[Boolean])]()) {
      case (acc: List[(String, Seq[Boolean])], word: String) =>
        checkForComment(word)
        acc :+ (word, Seq(checkForQuotes(word), inComment))
    }
  }

  val keywords: Seq[String] = Seq(
    "acquires",
    "adaptor",
    "all",
    "any",
    "append",
    "application",
    "attachment",
    "author",
    "become",
    "benefit",
    "brief",
    "briefly",
    "body",
    "call",
    "case",
    "capability",
    "command",
    "commands",
    "condition",
    "connector",
    "constant",
    "container",
    "contains",
    "context",
    "create",
    "described",
    "description",
    "details",
    "direct",
    "presents",
    "do",
    "domain",
    "else",
    "email",
    "end",
    "entity",
    "epic",
    "error",
    "event",
    "example",
    "execute",
    "explanation",
    "explained",
    "field",
    "fields",
    "file",
    "flow",
    "focus",
    "for",
    "foreach",
    "form",
    "from",
    "fully",
    "function",
    "graph",
    "group",
    "handler",
    "if",
    "import",
    "include",
    "index",
    "init",
    "inlet",
    "inlets",
    "input",
    "invariant",
    "items",
    "label",
    "link",
    "many",
    "mapping",
    "merge",
    "message",
    "module",
    "morph",
    "name",
    "nebula",
    "on",
    "one",
    "organization",
    "option",
    "optional",
    "options",
    "other",
    "outlet",
    "outlets",
    "output",
    "parallel",
    "pipe",
    "plant",
    "projector",
    "query",
    "range",
    "reference",
    "relationship",
    "remove",
    "replica",
    "reply",
    "repository",
    "requires",
    "required",
    "record",
    "result",
    "results",
    "return",
    "returns",
    "reverted",
    "router",
    "saga",
    "schema",
    "selects",
    "send",
    "sequence",
    "set",
    "show",
    "shown",
    "sink",
    "source",
    "split",
    "state",
    "step",
    "stop",
    "story",
    "streamlet",
    "table",
    "take",
    "tell",
    "term",
    "then",
    "title",
    "type",
    "url",
    "updates",
    "user",
    "value",
    "void",
    "when",
    "where",
    "with"
  )

  val triplePunctuation: Seq[String] = Seq("...", "???")

  val punctuation: Seq[String] = Seq(
    "@",
    "*",
    ",",
    ":",
    ".",
    "=",
    "!",
    "+",
    "?",
    "[",
    "]",
    "|",
    "{",
    "}",
    "(",
    ")"
  )

  val readability: Seq[String] = Seq(
    "and",
    "are",
    "as",
    "at",
    "by",
    "for",
    "from",
    "in",
    "is",
    "of",
    "so",
    "that",
    "to",
    "wants",
    "with"
  )

  val CUSTOM_KEYWORD_KEYWORD: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES
  val CUSTOM_KEYWORD_PUNCTUATION: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES
  val CUSTOM_KEYWORD_READABILITY: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD4_ATTRIBUTES
}
