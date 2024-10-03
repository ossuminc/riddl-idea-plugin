package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.TextRange
import com.ossuminc.riddl.plugins.idea.files.utils.{
  getWholeWordsSubstrings,
  highlightKeywordsInDoc
}
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*

class RiddlDocumentListener extends DocumentListener {
  override def documentChanged(event: DocumentEvent): Unit = {
    val doc = event.getDocument
    if doc != null then {
      val editors = EditorFactory.getInstance().getEditors(doc)
      if editors.nonEmpty then
        val newText = doc.getText(
          new TextRange(event.getOffset, event.getOffset + event.getNewLength)
        )
        val wholeWords = getWholeWordsSubstrings(
          doc.getText,
          newText,
          event.getOffset
        )
        highlightKeywordsInDoc(
          wholeWords
            .zip(
              RiddlTokenizer
                .tokenize(wholeWords.map(_._1).mkString(" "))
                .filter(!_._1.isBlank)
            )
            .map((wwTup, tokTup) => (wwTup._1, wwTup._2, tokTup._3)),
          editors.head
        )
    }
  }
}
