/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.structure

import com.intellij.ide.structureView.{StructureViewModel, StructureViewModelBase, StructureViewTreeElement}
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/** Structure view model for RIDDL files.
  *
  * Provides the tree structure showing RIDDL definitions hierarchy.
  */
class RiddlStructureViewModel(psiFile: PsiFile, editor: Editor)
    extends StructureViewModelBase(psiFile, editor, new RiddlFileStructureElement(psiFile))
    with StructureViewModel.ElementInfoProvider {

  override def getSorters: Array[Sorter] = Array(Sorter.ALPHA_SORTER)

  override def isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean =
    element.isInstanceOf[RiddlFileStructureElement]

  override def isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
    element match {
      case e: RiddlDefinitionElement => !e.hasChildren
      case _                         => false
    }
}
