package com.ossuminc.riddl.plugins.idea.files

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.ossuminc.riddl.plugins.idea.utils.RiddlIcon

import javax.swing.Icon

object RiddlLanguage extends Language("RIDDL")

class RiddlFileType extends LanguageFileType(RiddlLanguage) {

  override def getName: String = "RiddlFileType"

  override def getDescription: String =
    "Supports correct display of RIDDL (.riddl) files"

  override def getDefaultExtension: String = "riddl"

  override def getIcon: Icon = RiddlIcon(getClass)
}
