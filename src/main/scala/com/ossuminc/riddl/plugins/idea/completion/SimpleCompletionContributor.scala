// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.ossuminc.riddl.plugins.idea.completion

import com.intellij.codeInsight.completion.CompletionContributor

import com.intellij.codeInsight.completion._
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.parsing.PropertiesTokenTypes
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class SimpleCompletionContributor() //extends CompletionContributor {
{
  // extend(
  //  CompletionType.BASIC,
  //  PlatformPatterns.psiElement(PropertiesTokenTypes.VALUE_CHARACTERS),
  //  new CompletionProvider[CompletionParameters]() {
  //    override def addCompletions(
  //        parameters: CompletionParameters,
  //        context: ProcessingContext,
  //        result: CompletionResultSet
  //    ): Unit =
  //      result.addElement(LookupElementBuilder.create("HELLO"))
  //  }
  // )

}
