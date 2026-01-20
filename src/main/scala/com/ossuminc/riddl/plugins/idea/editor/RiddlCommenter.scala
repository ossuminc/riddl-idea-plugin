/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.editor

import com.intellij.lang.Commenter

/** Commenter for RIDDL files.
  *
  * Provides comment toggling support (Cmd+/ or Ctrl+/) for RIDDL files.
  *
  * RIDDL supports:
  *   - Line comments: // comment
  *   - Block comments: slash-star ... star-slash
  */
class RiddlCommenter extends Commenter {

  /** Line comment prefix. */
  override def getLineCommentPrefix: String = "// "

  /** Block comment prefix. */
  override def getBlockCommentPrefix: String = "/* "

  /** Block comment suffix. */
  override def getBlockCommentSuffix: String = " */"

  /** Commented block comment prefix (for nested comments). */
  override def getCommentedBlockCommentPrefix: String = null

  /** Commented block comment suffix (for nested comments). */
  override def getCommentedBlockCommentSuffix: String = null
}
