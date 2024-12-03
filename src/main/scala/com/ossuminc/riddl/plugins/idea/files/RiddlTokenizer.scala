package com.ossuminc.riddl.plugins.idea.files

import com.intellij.ide.highlighter.custom.CustomHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object RiddlTokenizer {
  val CUSTOM_KEYWORD_KEYWORD: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD2_ATTRIBUTES
  val CUSTOM_KEYWORD_PUNCTUATION: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD3_ATTRIBUTES
  val CUSTOM_KEYWORD_READABILITY: TextAttributesKey =
    CustomHighlighterColors.CUSTOM_KEYWORD4_ATTRIBUTES
}
