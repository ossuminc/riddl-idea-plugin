package com.ossuminc.riddl.plugins.idea

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow
import com.ossuminc.riddl.plugins.idea.ui.RiddlToolWindowFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait IdeaTestCase {
  object LightFixture extends BasePlatformTestCase

  def getTestRootDisposable: Disposable = LightFixture.getTestRootDisposable
}

class RiddlToolWindowFactorySpec
    extends AnyWordSpec
    with IdeaTestCase
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  "RiddlToolWindowFactorySpec" must {
    "output directory compilation" in {
      val factory = new RiddlToolWindowFactory()

      val proj = new MockProject(null, getTestRootDisposable)
      val window = new MockToolWindow(proj)

      factory.createToolWindowContent(proj, window) mustEqual ()
    }
  }
}
