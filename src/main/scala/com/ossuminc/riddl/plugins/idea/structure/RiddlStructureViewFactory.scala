/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.structure

import com.intellij.ide.structureView.{StructureViewBuilder, StructureViewModel, TreeBasedStructureViewBuilder}
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/** Factory for creating RIDDL structure views.
  *
  * The structure view shows a hierarchical outline of RIDDL definitions
  * in the current file, allowing quick navigation to definitions.
  */
class RiddlStructureViewFactory extends PsiStructureViewFactory {

  override def getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
    new TreeBasedStructureViewBuilder {
      override def createStructureViewModel(editor: Editor): StructureViewModel =
        new RiddlStructureViewModel(psiFile, editor)
    }
}
