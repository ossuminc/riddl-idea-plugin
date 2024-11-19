package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.TextRange
import com.ossuminc.riddl.plugins.idea.files.utils.{
  getWholeWordsSubstrings,
  highlightKeywordsOnChange
}
import com.ossuminc.riddl.plugins.idea.utils.highlightForErrorMessage
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.*
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*

import java.io.{File, PrintWriter}
import java.nio.file.Path
import scala.io.Source
import scala.util.Using

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

          if editor.getVirtualFile != null then
            val editorFilePath = editor.getVirtualFile.getPath
            getRiddlIdeaStates.allStates.values.toSeq
              .filter { state =>
                state.getTopLevelPath.exists(path =>
                  Path.of(path).compareTo(Path.of(editorFilePath)) <= 0
                )
              }
              .foreach { state =>
                state.getMessages
                  .filter(msg =>
                    editorFilePath.endsWith(msg.loc.source.origin)
                  )
                  .foreach { msg =>
                    runCommandForEditor(
                      state.getWindowNum,
                      editorFilePath,
                      true
                    )
                    highlightForErrorMessage(
                      state,
                      Right(msg.message),
                      Right((msg.loc, msg.kind.severity))
                    )
                  }
              }

        case _ => ()
  }
}
