package com.ossuminc.riddl.plugins.idea.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ossuminc.riddl.plugins.idea.files.RiddlDocumentListener
import kotlin.coroutines.Continuation

class RiddlProject extends ProjectActivity {
  override def execute(project: Project, continuation: Continuation[? >: kotlin.Unit]): Unit = {
    com.intellij.openapi.editor.EditorFactory.getInstance().getEventMulticaster.addDocumentListener(
      RiddlDocumentListener(), () => ()
    )
  }
}
