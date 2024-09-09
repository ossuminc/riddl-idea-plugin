package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomHighlighterColors
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.CustomHighlighterTokenType

object RiddlTokenizer {
  def tokenize(text: String): Seq[(String, Int, Int)] = {
    var currentIndex = 0
    tokenizeWithQuotesBoolean(text).map { case (word, inQuotes) =>
      if !inQuotes then {
        val tuple = (word, currentIndex, word.length)
        currentIndex += word.length
        tuple
      }
      else {
        currentIndex += word.length
        (word, -1, 0)
      }
    }
  }

  private def tokenizeWithQuotesBoolean(text: String): Seq[(String, Boolean)] = {
    var inQuotes: Boolean = false
    text.split("((?<=\\s+)|(?=\\s+))").toSeq.foldLeft(List[(String, Boolean)]()) {
      case (acc: List[(String, Boolean)], word: String) =>
        if word.endsWith("\"") && word.startsWith("\"") then
          inQuotes = false
          acc :+ (word, false)
        else if word.contains("\"") then {
          val tokBool = acc :+ (word, !inQuotes)
          inQuotes = !inQuotes
          tokBool
        }
        else acc :+ (word, inQuotes)
    }
  }

  val keywords = Seq("acquires", "adaptor", "all", "any", "append", "application", "author",
    "become", "benefit", "brief", "briefly", "body", "call", "case_", "capability", "command", "commands",
    "condition", "connector", "constant", "container", "contains", "context", "create", "described", "details",
    "direct", "presents", "do_", "domain", "else_", "email", "end_", "entity", "epic", "error", "event", "example",
    "execute", "explained", "field", "fields", "file", "flow", "focus", "for_", "foreach", "form", "from",
    "function", "graph", "group", "handler", "if_", "import_", "include", "index", "init", "inlet", "inlets",
    "input", "invariant", "items", "many", "mapping", "merge", "message", "morph", "name", "on", "one", "organization",
    "option", "optional", "options", "other", "outlet", "outlets", "output", "parallel", "pipe", "plant", "projector",
    "query", "range", "reference", "remove", "replica", "reply", "repository", "requires", "required", "record",
    "result", "results", "return_", "returns", "reverted", "router", "saga", "schema", "selects", "send", "sequence",
    "set", "show", "shown", "sink", "source", "split", "state", "step", "stop", "story", "streamlet", "table", "take",
    "tell", "term", "then_", "title", "type", "url", "updates", "user", "value", "void", "when", "where")

  val punctuation = Seq("asterisk", "at", "comma", "colon", "curlyOpen", "curlyClose", "dot", "equalsSign",
    "ellipsis", "ellipsisQuestion", "exclamation", "plus", "question", "quote", "roundOpen", "roundClose",
    "squareOpen", "squareClose", "undefinedMark", "verticalBar")

  val readability = Seq("and", "are", "as", "at", "by", "for", "from", "in", "is", "of", "on",
    "so", "that", "to", "wants", "with")

  val CUSTOM_KEYWORD2: TextAttributesKey = CustomHighlighterColors.CUSTOM_KEYWORD1_ATTRIBUTES
  val CUSTOM_KEYWORD3: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "CUSTOM_KEYWORD3_ATTRIBUTES", DefaultLanguageHighlighterColors.KEYWORD
  )
  val CUSTOM_KEYWORD4: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "CUSTOM_KEYWORD4_ATTRIBUTES", DefaultLanguageHighlighterColors.KEYWORD
  )
}

