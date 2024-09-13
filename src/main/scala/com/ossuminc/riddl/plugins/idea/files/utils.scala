package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Editor}
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*

object utils {
  def splitByBlanks(str: String): Array[String] = str.split("((?<=\\s)(?=\\S)|(?<=\\S)(?=\\s))")

  def getWholeWordsSubstrings(doc: String, eventText: String, start: Int): Seq[(String, Int)] =
    val splitText: Seq[String] = splitByBlanks(eventText).toSeq
    val indices: Seq[Int] = splitText.scanLeft(0)((acc, part) => {
      acc + part.length
    })
    indices.zip(splitText).filter(_._2.trim.nonEmpty).map { (index, text) =>
      val indexedStart = index + start
      val adjustedStart =
        if !doc.charAt(indexedStart).isWhitespace && doc.lastIndexWhere(_.isWhitespace, indexedStart) > -1 then
          doc.lastIndexWhere(_.isWhitespace, indexedStart) + 1
        else if doc.lastIndexWhere(_.isWhitespace, indexedStart) == -1 then 0
        else indexedStart

      val indexedEnd = indexedStart + text.length
      val adjustedEnd =
        if indexedEnd < doc.length && !doc.charAt(indexedEnd).isWhitespace then
          doc.indexWhere(_.isWhitespace, indexedEnd) match {
            case -1 => doc.length
            case idx => idx
          }
        else indexedEnd

      val numLeadingSpaces = "^\\s+".r.findFirstIn(text).map(_.length).getOrElse(0)
      val numTrailingSpaces = "\\s+$".r.findFirstIn(text).map(_.length).getOrElse(0)

      (
        doc.substring(adjustedStart + numLeadingSpaces, adjustedEnd - numTrailingSpaces).trim,
        adjustedStart + numLeadingSpaces
      )
    }

  def highlightKeywords(text: String, editor: Editor): Unit = {
    import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
    RiddlTokenizer.tokenize(text).filter(!_._1.isBlank).foreach {applyColorToToken(editor)}
  }

  def highlightKeywordsInDoc(tokens: Seq[(String, Int, Seq[Boolean])], editor: Editor): Unit = {
    tokens.foreach(applyColorToToken(editor))
  }

  private def applyColorToToken(editor: Editor)(token: String, index: Int, flags: Seq[Boolean]): Unit = flags match {
      case Seq(isQuoted, isComment) if isQuoted || isComment =>
        applyColourKey(editor)(
          if isComment then DefaultLanguageHighlighterColors.LINE_COMMENT
          else if isQuoted then DefaultLanguageHighlighterColors.STRING
          else DefaultLanguageHighlighterColors.IDENTIFIER,
          index,
          token.length,
          -1,
          ""
        )
      case Seq(_, _) if index > -1 && !token.isBlank =>
        if keywords.contains(token) then applyColourKey(editor)(CUSTOM_KEYWORD_KEYWORD, index, token.length, -1, "")
        else if punctuation.exists(token.contains) then
          punctuation.find(token.contains).foreach(punct =>
            applyColourKey(editor)(CUSTOM_KEYWORD_PUNCTUATION, index, token.length, token.indexOf(punct), punct)
          )
        else if readability.contains(token) then applyColourKey(editor)(CUSTOM_KEYWORD_READABILITY, index, token.length, -1, "")
        else applyColourKey(editor)(DefaultLanguageHighlighterColors.IDENTIFIER, index, token.length, -1, "")
    }

  private def applyColourKey(editor: Editor)(
    colorKey: TextAttributesKey,
    index: Int,
    length: Int,
    punctIndex: Int,
    punct: String
  ): Unit = {
    val (trueIndex: Int, trueLength: Int) =
      if punctIndex < 0 then (index, length) else (punctIndex + index, punct.length)

    editor.getMarkupModel.addRangeHighlighter(
      colorKey,
      trueIndex,
      trueIndex + trueLength,
      HighlighterLayer.SYNTAX,
      HighlighterTargetArea.EXACT_RANGE
    )
    editor.getContentComponent.repaint()
    ()
  }
}
