package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileEditor.{
  TextEditor,
  FileDocumentManager,
  FileEditorManager
}
import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Editor}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.markup.{
  HighlighterLayer,
  HighlighterTargetArea
}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
import com.ossuminc.riddl.plugins.idea.utils.highlightErrorForFile
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaStates

object utils {
  def splitByBlanks(str: String): Array[String] =
    str.split("((?<=\\s)(?=\\S)|(?<=\\S)(?=\\s))")

  def getWholeWordsSubstrings(
      doc: String,
      newText: String,
      start: Int
  ): Seq[(String, Int)] = {
    def getSubStringsWithIndices(offset: Int, text: String): (String, Int) = {
      val indexedStart = offset + start
      val adjustedStart =
        if !doc.charAt(indexedStart).isWhitespace && doc
            .lastIndexWhere(_.isWhitespace, indexedStart) > -1
        then doc.lastIndexWhere(_.isWhitespace, indexedStart) + 1
        else if doc.lastIndexWhere(_.isWhitespace, indexedStart) == -1 then 0
        else indexedStart

      val indexedEnd = indexedStart + text.length
      val adjustedEnd =
        if indexedEnd < doc.length && !doc.charAt(indexedEnd).isWhitespace then
          doc.indexWhere(_.isWhitespace, indexedEnd) match {
            case -1  => doc.length
            case idx => idx
          }
        else indexedEnd

      val numLeadingSpaces =
        "^\\s+".r.findFirstIn(text).map(_.length).getOrElse(0)
      val numTrailingSpaces =
        "\\s+$".r.findFirstIn(text).map(_.length).getOrElse(0)

      (
        doc
          .substring(
            adjustedStart + numLeadingSpaces,
            adjustedEnd - numTrailingSpaces
          )
          .trim,
        adjustedStart + numLeadingSpaces
      )
    }

    if newText.isBlank && newText != "\b" then
      Seq(
        getSubStringsWithIndices(
          if start > 0 && doc.charAt(start - 1).isWhitespace then -2
          else if start > 0 then -1
          else 0,
          ""
        ),
        getSubStringsWithIndices(
          if doc.charAt(start).isWhitespace || start == 0 then 0 else -1,
          ""
        )
      )
    else if !newText.isBlank then
      val splitText: Seq[String] = splitByBlanks(newText).toSeq
      val indices: Seq[Int] = splitText.scanLeft(0)((acc, part) => {
        acc + ("""\b*""".r.findFirstIn(part) match
          case Some(backspaces) => part.length - backspaces.length
          case None             => part.length
        )
      })
      indices
        .zip(splitText)
        .filter(_._2.trim.nonEmpty)
        .map(getSubStringsWithIndices)
    else if !doc.isBlank then {
      Seq(
        getSubStringsWithIndices(
          if doc.charAt(start).isWhitespace || start == 0 then 0 else -1,
          ""
        )
      )
    } else Seq()
  }

  def highlightKeywords(text: String, editor: Editor): Unit = {
    import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
    RiddlTokenizer.tokenize(text).filter(!_._1.isBlank).foreach {
      applyColorToToken(editor)
    }

  }

  def highlightKeywordsOnChange(
      // see applyColorToToken for names of elements in tokens
      tokens: Seq[(String, Int, Seq[Boolean])],
      editor: Editor
  ): Unit = tokens.foreach(applyColorToToken(editor))

  private def applyColorToToken(
      editor: Editor
  )(token: String, index: Int, flags: Seq[Boolean]): Unit = {
    def puncIndexInToken(puncList: Seq[String]): Int =
      token.indexOf(puncList.find(token.contains).getOrElse(""))

    def callApplyColourKey(): Unit = if keywords.contains(token) then
      applyColourKey(editor)(
        CUSTOM_KEYWORD_KEYWORD,
        index,
        token.length,
        -1
      )
    else if triplePunctuation.exists(token.contains) then
      applyColourKey(editor)(
        CUSTOM_KEYWORD_PUNCTUATION,
        index,
        token.length,
        puncIndexInToken(triplePunctuation)
      )
    else if punctuation.exists(token.contains) then
      token.zipWithIndex
        .filter(tokenCharWithIndex =>
          punctuation.contains(tokenCharWithIndex._1.toString)
        )
        .foreach((_, puncIndexInToken) =>
          applyColourKey(editor)(
            CUSTOM_KEYWORD_PUNCTUATION,
            index,
            1,
            puncIndexInToken
          )
        )
    else if readability.contains(token) then
      applyColourKey(editor)(
        CUSTOM_KEYWORD_READABILITY,
        index,
        token.length,
        -1
      )
    else
      applyColourKey(editor)(
        DefaultLanguageHighlighterColors.IDENTIFIER,
        index,
        token.length,
        -1
      )

    flags match {
      case Seq(isQuoted, _) if isQuoted =>
        callApplyColourKey()
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.STRING,
          index,
          token.length,
          -1
        )
      case Seq(_, isComment) if isComment =>
        applyColourKey(editor)(
          DefaultLanguageHighlighterColors.LINE_COMMENT,
          index,
          token.length,
          -1
        )
      case Seq(_, _) if index > -1 && !token.isBlank =>
        callApplyColourKey()
    }
  }

  private def applyColourKey(editor: Editor)(
      colorKey: TextAttributesKey,
      index: Int,
      length: Int,
      puncIndex: Int
  ): Unit = {
    val trueIndex: Int =
      if puncIndex < 0 then index
      else puncIndex + index

    editor.getMarkupModel.getAllHighlighters
      .find(_.getStartOffset == trueIndex)
      .foreach(highlighter =>
        editor.getMarkupModel.removeHighlighter(highlighter)
      )

    editor.getMarkupModel.addRangeHighlighter(
      colorKey,
      trueIndex,
      trueIndex + length,
      HighlighterLayer.FIRST,
      HighlighterTargetArea.EXACT_RANGE
    )
    editor.getContentComponent.repaint()
  }

  def highlightKeywordsAndErrorsForFile(
      source: FileEditorManager,
      file: VirtualFile
  ): Unit = source
    .getAllEditors(file)
    .foreach { te =>
      val doc = FileDocumentManager.getInstance().getDocument(file)
      if doc != null then {
        te match {
          case textEditor: TextEditor =>
            textEditor.getEditor.getMarkupModel.removeAllHighlighters()
            highlightKeywords(doc.getText, textEditor.getEditor)
            getRiddlIdeaStates.allStates
              .foldRight(Seq[RiddlIdeaSettings.State]()) { (tup, acc) =>
                if tup._2.getMessages.exists(
                    _.loc.source.root.path == file.getPath
                  )
                then acc :+ tup._2
                else acc
              }
              .foreach(state =>
                highlightErrorForFile(state, file.getName)
              )
        }
      }
    }
}
