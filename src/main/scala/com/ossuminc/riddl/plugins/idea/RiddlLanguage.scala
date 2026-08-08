/*
 * Copyright 2024-2026 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ossuminc.riddl.plugins.idea

import com.intellij.lang.Language

/** The RIDDL language definition.
  *
  * This is the single source of truth for the RIDDL language definition in the IntelliJ platform.
  * Use [[RiddlLanguage.INSTANCE]] wherever the platform wants a `Language`.
  *
  * This is deliberately a `class` with a companion holding the singleton, rather than an `object`
  * extending `Language` directly. Scala emits static forwarders on an object's mirror class for
  * every inherited public method, so `object RiddlLanguage extends Language` produced 28 of them --
  * including `Language.findMyFileType`, `unregisterLanguage`, `unregisterDialect` and
  * `UserDataHolderBase.getUserMap`, all `@ApiStatus.Internal`. The IntelliJ Plugin Verifier reads
  * bytecode, so it reported those as internal-API usages even though no such call appears in this
  * source. A private constructor keeps the singleton guarantee the object gave us.
  */
final class RiddlLanguage private () extends Language("RIDDL")

object RiddlLanguage {
  val INSTANCE: RiddlLanguage = new RiddlLanguage()
}
