package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.ossuminc.riddl.plugins.idea.files.utils.{
  getWholeWordsSubstrings,
  highlightKeywordsOnChange
}
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaStates
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.runCommandForEditor

class RiddlDocumentListener extends DocumentListener {
  override def documentChanged(event: DocumentEvent): Unit = {
    val doc = event.getDocument

    val editors = EditorFactory.getInstance().getEditors(doc)
    if editors.nonEmpty then
      editors.find(_.getDocument == doc) match
        case Some(editor) if doc.getText.nonEmpty =>
          val newText = doc.getText(
            new TextRange(event.getOffset, event.getOffset + event.getNewLength)
          )
          val wholeWords: Seq[(String, Int)] = getWholeWordsSubstrings(
            doc.getText,
            newText,
            event.getOffset
          )
          highlightKeywordsOnChange(
            wholeWords
              .zip(
                RiddlTokenizer
                  .tokenize(wholeWords.map(_._1).mkString(" "))
                  .filter(!_._1.isBlank)
              )
              .map((wwTup, tokTup) => (wwTup._1, wwTup._2, tokTup._3)),
            editor
          )
          getRiddlIdeaStates.allStates
            .find(
              _._2.getConfPath == FileDocumentManager
                .getInstance()
                .getFile(doc)
                .getPath
            )
            .foreach(state =>
              runCommandForEditor(state._1, Some(state._2.getConfPath))
            )

        case _ => ()
  }
}
