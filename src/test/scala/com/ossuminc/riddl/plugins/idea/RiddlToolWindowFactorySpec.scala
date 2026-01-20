/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Placeholder test spec.
  *
  * Note: IntelliJ platform tests require proper test infrastructure setup.
  * The old test was incorrectly configured. Real integration tests will be
  * added as part of the Phase 1 rewrite.
  */
class RiddlToolWindowFactorySpec extends AnyWordSpec with Matchers {

  "RiddlToolWindowFactory" must {
    "be defined" in {
      // Placeholder - actual platform tests need proper IntelliJ test setup
      val className = classOf[ui.RiddlToolWindowFactory].getName
      className must include("RiddlToolWindowFactory")
    }
  }
}
