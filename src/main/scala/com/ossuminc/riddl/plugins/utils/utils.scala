package com.ossuminc.riddl.plugins

import com.intellij.notification.{Notification, NotificationType, Notifications}
import com.intellij.openapi.application.{Application, ApplicationManager}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.ossuminc.riddl.plugins.idea.settings.RiddlIdeaSettings

import java.awt.GridBagConstraints

//case class RiddlIdeaPluginLogger(override val withHighlighting: Boolean = true)
//    extends Logger {
//  import com.ossuminc.riddl.plugins.utils.getRiddlIdeaState
//
//  override def write(level: Logging.Lvl, s: String): Unit = {
//    getRiddlIdeaState.getState.appendOutput(s)
//  }
//}

package object utils {
  object ManagerBasedGetterUtils {
    val application: Application = ApplicationManager.getApplication

    def getProject: Project = ProjectManager.getInstance().getOpenProjects.head

    def getRiddlIdeaStates: RiddlIdeaSettings.States =
      application
        .getService(
          classOf[RiddlIdeaSettings]
        )
        .getState

    def getRiddlIdeaState(numToolWindow: Int): RiddlIdeaSettings.State =
      getRiddlIdeaStates.getState(
        numToolWindow
      )
  }

  object CreationUtils {
    def createGBCs(
        gridX: Int,
        gridY: Int,
        weightX: Int,
        wightY: Int,
        fill: Int
    ): GridBagConstraints = {
      val newGBCs = new GridBagConstraints()
      newGBCs.gridx = gridX
      newGBCs.gridy = gridY
      newGBCs.weightx = weightX
      newGBCs.weighty = wightY
      newGBCs.fill = fill
      newGBCs
    }
  }

  def displayNotification(text: String): Unit = Notifications.Bus.notify(
    new Notification(
      "Riddl Plugin Notification",
      text,
      NotificationType.INFORMATION
    )
  )

  def genWindowName(windowNumber: Int): String =
    if windowNumber == 0 then "riddlc"
    else s"riddlc ($windowNumber)"
}
