package com.ossuminc.riddl.plugins.idea.files

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.{LanguageFileType, PlainTextLanguage}
import com.intellij.openapi.util.IconLoader
import com.ossuminc.riddl.plugins.utils.RiddlIcon

import javax.swing.Icon

object RiddlLanguage extends Language("RIDDL")

class RiddlFileType extends LanguageFileType(RiddlLanguage){

  override def getName: String = "RiddlFileType"

  override def getDescription: String = "instantiates .riddl files as types in IDEA"

  override def getDefaultExtension: String = "riddl"

  override def getIcon: Icon = RiddlIcon(getClass)
}

object RiddlFileType {
  val INSTANCE: RiddlFileType = new RiddlFileType()
}