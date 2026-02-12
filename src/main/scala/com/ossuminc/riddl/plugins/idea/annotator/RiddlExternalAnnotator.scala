/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.annotator

import com.intellij.lang.annotation.{AnnotationHolder, ExternalAnnotator, HighlightSeverity}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.ossuminc.riddl.language.Messages.{Message, Error, Warning, Info, SevereError, MissingWarning, StyleWarning, UsageWarning}
import com.ossuminc.riddl.RiddlLib
import com.ossuminc.riddl.utils.{NullLogger, pc}

/** External annotator for RIDDL files.
  *
  * This annotator runs asynchronously in the background to validate RIDDL
  * syntax and semantics, then applies error/warning annotations to the editor.
  *
  * The ExternalAnnotator pattern is preferred for potentially slow operations
  * like parsing, as it doesn't block the UI thread.
  */
class RiddlExternalAnnotator
    extends ExternalAnnotator[RiddlAnnotationInfo, RiddlAnnotationResult] {

  /** Collect information needed for annotation (runs on UI thread, should be fast). */
  override def collectInformation(file: PsiFile): RiddlAnnotationInfo =
    RiddlAnnotationInfo(file.getText, file.getVirtualFile.getPath)

  /** Collect additional information from editor (optional). */
  override def collectInformation(
      file: PsiFile,
      editor: Editor,
      hasErrors: Boolean
  ): RiddlAnnotationInfo = collectInformation(file)

  /** Perform the actual annotation work (runs on background thread).
    *
    * Uses RiddlLib.validateString() for full semantic validation (parse +
    * resolution + validation passes). If the document fails to parse as a
    * complete Root (e.g. it's an include fragment), falls back to
    * RiddlLib.parseNebula() which tolerates partial definitions.
    */
  override def doAnnotate(info: RiddlAnnotationInfo): RiddlAnnotationResult =
    if info.text.isEmpty then RiddlAnnotationResult(Seq.empty)
    else
      pc.withLogger(NullLogger()) { _ =>
        val vr = RiddlLib.validateString(
          info.text, info.filePath
        )(using pc)
        if vr.parseErrors.isEmpty then
          // Full validation succeeded — return all messages
          RiddlAnnotationResult(vr.all)
        else
          // Parse as Root failed — try fragment parsing via nebula
          RiddlLib.parseNebula(
            info.text, info.filePath
          )(using pc) match
            case Right(_) =>
              RiddlAnnotationResult(Seq.empty)
            case Left(messages) =>
              RiddlAnnotationResult(messages.toSeq)
      }

  /** Apply the annotations to the editor (runs on UI thread). */
  override def apply(
      file: PsiFile,
      result: RiddlAnnotationResult,
      holder: AnnotationHolder
  ): Unit =
    result.messages.foreach { msg =>
      val severity = mapSeverity(msg)
      val range = calculateRange(file, msg)

      holder
        .newAnnotation(severity, msg.message)
        .range(range)
        .tooltip(formatTooltip(msg))
        .create()
    }

  /** Map RIDDL message severity to IntelliJ highlight severity. */
  private def mapSeverity(msg: Message): HighlightSeverity =
    msg.kind match
      case SevereError     => HighlightSeverity.ERROR
      case Error           => HighlightSeverity.ERROR
      case Warning         => HighlightSeverity.WARNING
      case MissingWarning  => HighlightSeverity.WARNING
      case UsageWarning    => HighlightSeverity.WEAK_WARNING
      case StyleWarning    => HighlightSeverity.WEAK_WARNING
      case Info            => HighlightSeverity.INFORMATION

  /** Calculate the text range for the annotation. */
  private def calculateRange(file: PsiFile, msg: Message): TextRange =
    val textLength = file.getTextLength
    val start = math.min(math.max(0, msg.loc.offset), textLength)
    val end = math.min(math.max(start, msg.loc.endOffset), textLength)

    // If start equals end, extend to show at least one character
    if start == end && start < textLength then TextRange.create(start, start + 1)
    else if start == end then TextRange.create(math.max(0, start - 1), start)
    else TextRange.create(start, end)

  /** Format the tooltip message with context. */
  private def formatTooltip(msg: Message): String =
    val kindName = msg.kind.toString
    val location = s"Line ${msg.loc.line}, Column ${msg.loc.col}"
    s"<html><b>$kindName</b>: ${msg.message}<br><i>$location</i></html>"
}

/** Information collected from the file for annotation. */
case class RiddlAnnotationInfo(text: String, filePath: String)

/** Result of the annotation analysis. */
case class RiddlAnnotationResult(messages: Seq[Message])
