package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.TextRange
import com.ossuminc.riddl.plugins.idea.files.utils.{
  highlightKeywordsOnChange,
  getWholeWordsSubstrings
}
import com.ossuminc.riddl.plugins.idea.utils.highlightErrorForFile
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.*
import com.ossuminc.riddl.plugins.idea.utils.ParsingUtils.*
import com.ossuminc.riddl.plugins.idea.files.RiddlTokenizer.*
import java.nio.file.Path

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
            val editorFilePath =
              Path.of(editor.getVirtualFile.getPath).toFile.getPath
            getRiddlIdeaStates.allStates.values.toSeq
              .filter(state =>
                state.getParsedPaths.exists(_.toFile.getPath == editorFilePath)
              )
              .foreach(state =>
                state.getParsedPaths
                  .map { path =>
                    println(path.toFile.getPath)
                    println(editorFilePath)
                    path
                  }
                  .filter(path => path.toFile.getPath == editorFilePath)
                  .foreach(_ =>
                    println("editor edited")
                    runCommandForEditor(state.getWindowNum)
                    highlightErrorForFile(state, editor.getVirtualFile.getName)
                  )
              )

        case _ => ()
  }
}
