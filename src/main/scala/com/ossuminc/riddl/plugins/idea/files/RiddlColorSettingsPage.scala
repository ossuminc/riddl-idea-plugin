package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomHighlighterColors
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

object RiddlColorKeywords {
  val CUSTOM_KEYWORD_READABILITY: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES
  val CUSTOM_KEYWORD_PUNCTUATION: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES
  val CUSTOM_KEYWORD_KEYWORD: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD4_ATTRIBUTES
}

class RiddlColorSettingsPage extends ColorSettingsPage {
  override def getIcon: Icon = RiddlIcon(getClass)

  override def getHighlighter: SyntaxHighlighter = new PlainSyntaxHighlighter()

  override def getAttributeDescriptors: Array[AttributesDescriptor] = Array(
    new AttributesDescriptor(
      "Keywords",
      RiddlColorKeywords.CUSTOM_KEYWORD_KEYWORD
    ),
    new AttributesDescriptor(
      "Punctuation",
      RiddlColorKeywords.CUSTOM_KEYWORD_PUNCTUATION
    ),
    new AttributesDescriptor(
      "Readability",
      RiddlColorKeywords.CUSTOM_KEYWORD_READABILITY
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
