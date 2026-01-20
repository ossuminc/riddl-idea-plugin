/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{DialogWrapper, Messages}
import com.intellij.psi.PsiElement
import com.intellij.ui.components.{JBLabel, JBTextArea}
import com.intellij.util.ui.JBUI
import com.ossuminc.riddl.plugins.idea.mcp.RiddlMcpService
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings

import java.awt.{BorderLayout, Dimension}
import javax.swing.{JComponent, JPanel, JScrollPane}

/** Intention action to generate RIDDL from a natural language description.
  *
  * Available via Alt+Enter in RIDDL files.
  */
class GenerateFromDescriptionIntention extends PsiElementBaseIntentionAction with IntentionAction {

  override def getText: String = "Generate RIDDL from description..."

  override def getFamilyName: String = "RIDDL AI Assistance"

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    // Only available in RIDDL files when MCP is enabled
    val settings = RiddlIdeaSettings.getInstance()
    val file = element.getContainingFile
    file != null &&
    file.getName.endsWith(".riddl") &&
    settings.getMcpEnabled
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    // Show dialog to get description
    val dialog = new GenerateDescriptionDialog(project)
    if dialog.showAndGet() then {
      val description = dialog.getDescription
      if description.nonEmpty then generateAndInsert(project, editor, description)
    }
  }

  private def generateAndInsert(project: Project, editor: Editor, description: String): Unit = {
    val mcpService = RiddlMcpService.getInstance(project)

    // Run in background to avoid blocking UI
    ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
      override def run(): Unit = {
        mcpService.generateFromDescription(description) match {
          case Right(result) =>
            if result.generatedRiddl.nonEmpty then {
              // Insert on EDT
              ApplicationManager.getApplication.invokeLater(new Runnable {
                override def run(): Unit = {
                  WriteCommandAction.runWriteCommandAction(
                    project,
                    new Runnable {
                      override def run(): Unit = {
                        val document = editor.getDocument
                        val offset = editor.getCaretModel.getOffset

                        // Insert the generated RIDDL
                        val textToInsert =
                          if offset > 0 && document.getText.charAt(offset - 1) != '\n' then
                            "\n" + result.generatedRiddl
                          else result.generatedRiddl

                        document.insertString(offset, textToInsert)

                        // Move caret to end of inserted text
                        editor.getCaretModel.moveToOffset(offset + textToInsert.length)
                      }
                    }
                  )
                }
              })
            } else {
              showError(project, "No RIDDL generated from the description.")
            }
          case Left(error) =>
            showError(project, s"Failed to generate RIDDL: ${error.message}")
        }
      }
    })
  }

  private def showError(project: Project, message: String): Unit = {
    ApplicationManager.getApplication.invokeLater(new Runnable {
      override def run(): Unit = {
        Messages.showErrorDialog(project, message, "RIDDL Generation Failed")
      }
    })
  }
}

/** Dialog for entering a description to generate RIDDL from. */
class GenerateDescriptionDialog(project: Project) extends DialogWrapper(project) {

  private val descriptionArea = new JBTextArea(8, 60)

  init()
  setTitle("Generate RIDDL from Description")

  override def createCenterPanel(): JComponent = {
    val panel = new JPanel(new BorderLayout(0, JBUI.scale(8)))

    val label = new JBLabel("Enter a description of what you want to model:")
    panel.add(label, BorderLayout.NORTH)

    descriptionArea.setLineWrap(true)
    descriptionArea.setWrapStyleWord(true)
    val scrollPane = new JScrollPane(descriptionArea)
    scrollPane.setPreferredSize(new Dimension(500, 200))
    panel.add(scrollPane, BorderLayout.CENTER)

    val hintLabel = new JBLabel(
      "<html><i>Example: A user management system with users who have names and emails, " +
        "and can be created, updated, and deleted.</i></html>"
    )
    panel.add(hintLabel, BorderLayout.SOUTH)

    panel
  }

  def getDescription: String = descriptionArea.getText.trim
}
