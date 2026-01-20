/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.files

import com.intellij.openapi.options.colors.{
  AttributesDescriptor,
  ColorDescriptor,
  ColorSettingsPage
}
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.ossuminc.riddl.plugins.idea.RiddlIcons
import com.ossuminc.riddl.plugins.idea.highlighting.{RiddlColors, RiddlSyntaxHighlighter}

import scala.jdk.CollectionConverters.*
import java.util
import javax.swing.Icon

/** Color settings page for RIDDL syntax highlighting.
  *
  * Allows users to customize RIDDL syntax colors in
  * Settings > Editor > Color Scheme > RIDDL.
  */
class RiddlColorSettingsPage extends ColorSettingsPage {

  override def getIcon: Icon = RiddlIcons.FILE

  override def getHighlighter: SyntaxHighlighter = new RiddlSyntaxHighlighter()

  override def getAttributeDescriptors: Array[AttributesDescriptor] =
    RiddlColorSettingsPage.DESCRIPTORS

  override def getDemoText: String = RiddlColorSettingsPage.DEMO_TEXT

  override def getAdditionalHighlightingTagToDescriptorMap: util.Map[String, TextAttributesKey] =
    Map.empty[String, TextAttributesKey].asJava

  override def getColorDescriptors: Array[ColorDescriptor] = Array.empty

  override def getDisplayName: String = "RIDDL"
}

object RiddlColorSettingsPage {

  val DESCRIPTORS: Array[AttributesDescriptor] = Array(
    new AttributesDescriptor("Keywords", RiddlColors.KEYWORD),
    new AttributesDescriptor("Identifiers", RiddlColors.IDENTIFIER),
    new AttributesDescriptor("Readability words", RiddlColors.READABILITY),
    new AttributesDescriptor("Punctuation", RiddlColors.PUNCTUATION),
    new AttributesDescriptor("Predefined types", RiddlColors.PREDEFINED),
    new AttributesDescriptor("Comments", RiddlColors.COMMENT),
    new AttributesDescriptor("Strings", RiddlColors.STRING),
    new AttributesDescriptor("Documentation", RiddlColors.MARKDOWN),
    new AttributesDescriptor("Literal code", RiddlColors.LITERAL_CODE),
    new AttributesDescriptor("Numbers", RiddlColors.NUMERIC)
  )

  val DEMO_TEXT: String =
    """// RIDDL Color Settings Demo
      |domain MyDomain is {
      |  type UserId is Id(User)
      |  type Name is String(1, 64)
      |  type Age is Integer(0, 150)
      |
      |  context UserContext is {
      |    entity User is {
      |      state UserState of User.Type is {
      |        handler UserHandler is {
      |          on command CreateUser {
      |            "Create a new user"
      |            set field name to @CreateUser.name
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin
}
