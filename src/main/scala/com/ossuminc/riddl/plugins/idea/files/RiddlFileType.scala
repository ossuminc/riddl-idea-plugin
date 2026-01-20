/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.fileTypes.LanguageFileType
import com.ossuminc.riddl.plugins.idea.{RiddlIcons, RiddlLanguage}

import javax.swing.Icon

/** The RIDDL file type definition.
  *
  * Registered in plugin.xml to associate .riddl files with RIDDL language support.
  */
class RiddlFileType extends LanguageFileType(RiddlLanguage) {

  override def getName: String = "RIDDL"

  override def getDescription: String =
    "RIDDL (Reactive Interface to Domain Definition Language) file"

  override def getDefaultExtension: String = "riddl"

  override def getIcon: Icon = RiddlIcons.FILE
}

object RiddlFileType {
  val INSTANCE = new RiddlFileType()
}
