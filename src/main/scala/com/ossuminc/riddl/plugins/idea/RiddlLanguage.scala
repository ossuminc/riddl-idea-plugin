/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea

import com.intellij.lang.Language

/** The RIDDL language singleton.
  *
  * This is the single source of truth for the RIDDL language definition
  * in the IntelliJ platform.
  */
object RiddlLanguage extends Language("RIDDL") {
  val INSTANCE: RiddlLanguage.type = this
}