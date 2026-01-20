/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Icon definitions for the RIDDL plugin.
  *
  * Icons are loaded from src/main/resources/images/
  */
object RiddlIcons {

  /** The main RIDDL file type icon (16x16). */
  val FILE: Icon = IconLoader.getIcon("/images/RIDDL-icon.jpg", getClass)

  /** The RIDDL logo for tool windows and other UI elements. */
  val LOGO: Icon = IconLoader.getIcon("/images/RIDDL-Logo.jpg", getClass)
}