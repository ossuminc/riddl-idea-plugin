package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.TextRange
import com.ossuminc.riddl.plugins.idea.files.utils.{
  getWholeWordsSubstrings,
  highlightKeywordsOnChange
}
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.highlightErrorForFile

class RiddlDocumentListener extends DocumentListener {
  override def documentChanged(event: DocumentEvent): Unit = {
    val doc = event.getDocument
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
      highlightKeywordsOnChange(
        wholeWords
          .zip(
            RiddlTokenizer
              .tokenize(wholeWords.map(_._1).mkString(" "))
              .filter(!_._1.isBlank)
          )
          .map((wwTup, tokTup) => (wwTup._1, wwTup._2, tokTup._3)),
        editors.head
      )
    editors.foreach(editor =>
      if editor.getVirtualFile != null then
        highlightErrorForFile(
          getRiddlIdeaStates.allStates
            .find(state =>
              editor.getVirtualFile.getPath.contains(state._2.getConfPath)
            )
            .getOrElse(-1 -> RiddlIdeaSettings.State(-1))
            ._2,
          editor.getVirtualFile.getName
        )
    )
  }
}
