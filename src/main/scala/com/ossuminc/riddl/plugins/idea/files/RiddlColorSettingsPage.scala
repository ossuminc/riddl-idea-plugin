package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomFileHighlighter
import com.intellij.openapi.options.colors.{
  AttributesDescriptor,
  ColorDescriptor,
  ColorSettingsPage
}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.{
  PlainSyntaxHighlighter,
  SyntaxHighlighter
}
import com.ossuminc.riddl.plugins.idea.utils.RiddlIcon

import scala.jdk.CollectionConverters.*
import java.util
import javax.swing.Icon

class RiddlColorSettingsPage extends ColorSettingsPage {
  override def getIcon: Icon = RiddlIcon(getClass)

  override def getHighlighter: SyntaxHighlighter = new PlainSyntaxHighlighter()

  override def getAttributeDescriptors: Array[AttributesDescriptor] = Array(
    new AttributesDescriptor("Keywords", RiddlTokenizer.CUSTOM_KEYWORD_KEYWORD),
    new AttributesDescriptor(
      "Punctuation",
      RiddlTokenizer.CUSTOM_KEYWORD_PUNCTUATION
    ),
    new AttributesDescriptor(
      "Readability",
      RiddlTokenizer.CUSTOM_KEYWORD_READABILITY
    )
  )

  override def getDemoText: String = "RIDDL"

  override def getAdditionalHighlightingTagToDescriptorMap
      : util.Map[String, TextAttributesKey] =
    Map[String, TextAttributesKey]().asJava

  override def getColorDescriptors: Array[ColorDescriptor] =
    Array[ColorDescriptor]()

  override def getDisplayName: String = "RIDDL"
}
