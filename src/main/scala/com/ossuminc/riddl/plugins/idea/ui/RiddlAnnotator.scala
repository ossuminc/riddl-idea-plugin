package com.ossuminc.riddl.plugins.idea.ui

import com.intellij.lang.annotation.{AnnotationHolder, Annotator, HighlightSeverity}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.ossuminc.riddl.plugins.idea.utils.ManagerBasedGetterUtils.getRiddlIdeaStates

import scala.collection.mutable

class RiddlAnnotator extends Annotator {
  override def annotate(element: PsiElement, holder: AnnotationHolder): Unit = {
    val elementPath = element.getContainingFile.getVirtualFile.getPath

    getRiddlIdeaStates.allStates.foreach { (_, state) =>
      state.getMessagesForEditor
        .getOrElse(elementPath, mutable.Seq())
        .foreach { msg =>
          if elementPath.endsWith(msg.loc.source.origin) then {

            val range = new TextRange(
              msg.loc.offset,
              msg.loc.offset + msg.loc.source.annotateErrorLine(msg.loc).length
            )
            val severity = HighlightSeverity.WARNING
            val textAttributes = TextAttributesKey.createTextAttributesKey(
              "CUSTOM_LANGUAGE_TODO_WARNING"
            )

            holder
              .newAnnotation(severity, msg.format)
              .range(range)
              .textAttributes(textAttributes)
              .create()
          }
        }
    }
  }
}
