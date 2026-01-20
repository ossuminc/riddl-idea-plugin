/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.highlighting

import com.intellij.openapi.fileTypes.{SyntaxHighlighter, SyntaxHighlighterFactory}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** Factory for creating RIDDL syntax highlighters.
  *
  * Registered in plugin.xml to provide syntax highlighting for RIDDL files.
  */
class RiddlSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

  override def getSyntaxHighlighter(
      project: Project,
      virtualFile: VirtualFile
  ): SyntaxHighlighter = new RiddlSyntaxHighlighter()
}
